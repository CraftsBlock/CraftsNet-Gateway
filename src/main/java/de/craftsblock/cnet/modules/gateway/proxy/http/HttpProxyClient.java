package de.craftsblock.cnet.modules.gateway.proxy.http;

import de.craftsblock.cnet.modules.gateway.entities.Cluster;
import de.craftsblock.cnet.modules.gateway.entities.ClusterChild;
import de.craftsblock.craftsnet.api.http.Exchange;
import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.http.Response;
import de.craftsblock.craftsnet.api.http.encoding.StreamEncoder;
import de.craftsblock.craftsnet.api.utils.Scheme;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpProxyClient {

    private final Cluster cluster;
    private final ClusterChild child;

    private final HttpClient httpClient;

    public HttpProxyClient(Cluster cluster, ClusterChild child) {
        if (!child.getScheme().isSameFamily(Scheme.HTTP))
            throw new IllegalArgumentException("Can only create http clients from http scheme family! Provided: " + child.getScheme());

        this.cluster = cluster;
        this.child = child;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(child.getHttpConnectTimeout())
                .followRedirects(child.getHttpRedirectPolicy())
                .build();
    }

    public void proxyRequest(Exchange exchange) throws IOException, InterruptedException {
        Request incoming = exchange.request();
        Response outgoing = exchange.response();

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(buildRequestURI(incoming)).version(HttpClient.Version.HTTP_1_1);
        HttpRequest.BodyPublisher body = buildBodyPublisher(incoming);
        requestBuilder.method(incoming.getHttpMethod().name(), body);

        incoming.getHeaders().forEach((key, values) -> {
            try {
                switch (key.toLowerCase()) {
                    case "accept-encoding" -> requestBuilder.setHeader(
                            "Accept-Encoding",
                            "gzip, deflate" + (child.getGateway().streamEncoderRegistry().isRegistered("br") ? ", br" : "") + ", zstd"
                    );

                    case "etag", "if-match ", " if-none-match ",
                         "if-modified-since", "if-unmodified-since" -> {
                        if (child.isHttpCacheAllowed())
                            requestBuilder.setHeader(key, String.join(", ", values));
                    }
                    default -> requestBuilder.setHeader(key, String.join(", ", values));
                }
            } catch (IllegalArgumentException e) {
                if (!e.getMessage().startsWith("restricted")) throw new RuntimeException(e);
            }
        });

        HttpRequest request = requestBuilder.build();
        HttpResponse<InputStream> response = httpClient.send(
                request,
                responseInfo -> HttpResponse.BodySubscribers.ofInputStream()
        );

        outgoing.setCode(response.statusCode());
        response.headers().map().forEach((key, values) -> {
            switch (key.toLowerCase()) {
                case "content-length", "content-encoding" -> {
                }
                case "content-type" -> outgoing.setHeader(key, values.get(0));
                default -> values.forEach(value -> outgoing.addHeader(key, value));
            }
        });

        try (InputStream rawBody = response.body()) {
            String encoding = response.headers().firstValue("Content-Encoding").orElse("identity");
            StreamEncoder encoder = cluster.getGateway().streamEncoderRegistry().retrieveEncoder(encoding);
            if (encoder == null)
                throw new UnsupportedEncodingException("Received body with unsupported encoding (" + encoding +
                        ") from upstream server (" + buildRequestURI(incoming) + ")");

            try (InputStream responseBody = encoder.encodeInputStream(rawBody);) {
                outgoing.print(responseBody);
            }
        }
    }

    private URI buildRequestURI(Request incoming) {
        String path = child.wrapBase(cluster.removeBaseFromPath(incoming.getUrl()));
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        String baseUrl = child.getScheme().getName() + "://" + child.getHost() + ":" + child.getPort() + normalizedPath;
        String queryString = buildQueryString(incoming.getQueryParams());
        return URI.create(baseUrl + queryString);
    }

    private HttpRequest.BodyPublisher buildBodyPublisher(Request request) {
        if (!request.hasBody()) return HttpRequest.BodyPublishers.noBody();

        try {
            InputStream body = request.getRawBody();
            return HttpRequest.BodyPublishers.ofInputStream(() -> body);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildQueryString(Map<String, String> paramsMap) {
        if (paramsMap.isEmpty()) return "";
        return "?" + paramsMap.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }

}
