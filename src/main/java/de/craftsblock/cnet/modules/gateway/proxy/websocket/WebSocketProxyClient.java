package de.craftsblock.cnet.modules.gateway.proxy.websocket;

import de.craftsblock.cnet.modules.gateway.entities.Cluster;
import de.craftsblock.cnet.modules.gateway.entities.ClusterChild;
import de.craftsblock.craftsnet.api.utils.Scheme;
import de.craftsblock.craftsnet.api.websocket.WebSocketClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.List;

public class WebSocketProxyClient {

    private final Cluster cluster;
    private final ClusterChild child;

    private final HttpClient httpClient;
    private final WebSocket webSocket;
    private final WebSocketListener webSocketListener;
    private final WebSocketClient counterpart;

    private final Scheme scheme;
    private final String host;
    private final int port;
    private final String path;
    private final String url;

    public WebSocketProxyClient(Cluster cluster, ClusterChild child, WebSocketClient counterpart,
                                Scheme scheme, String host, int port, String path) {
        if (!scheme.isSameFamily(Scheme.WS))
            throw new IllegalArgumentException("Can only create web socket clients from ws scheme family! Provided: " + scheme);

        this.cluster = cluster;
        this.child = child;
        this.counterpart = counterpart;

        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.path = path;

        this.httpClient = HttpClient.newHttpClient();
        this.webSocketListener = new WebSocketListener(this, counterpart);

        WebSocket.Builder builder = httpClient.newWebSocketBuilder();
        List<String> protocols = counterpart.getHeaders().get("Sec-WebSocket-Protocol");
        if (protocols != null && !protocols.isEmpty())
            builder.subprotocols(protocols.get(0), protocols.stream().skip(1).toArray(String[]::new));

        this.url = scheme.getName() + "://" + host + ":" + port + (path.startsWith("/") ? "" : "/") + path;
        this.webSocket = builder.buildAsync(URI.create(this.url), this.webSocketListener).join();
    }

    public void kill() {
        this.webSocket.abort();
    }

    public void close(int statusCode, String message) {
        this.webSocket.sendClose(statusCode, message);
    }

    public void sendMessage(String message) {
        this.webSocket.sendText(message, true);
    }

    public void sendMessage(byte[] message) {
        this.webSocket.sendBinary(ByteBuffer.wrap(message), true);
    }

    public void sendPing(byte[] message) {
        this.webSocket.sendPing(ByteBuffer.wrap(message));
    }

    public void sendPong(byte[] message) {
        this.webSocket.sendPong(ByteBuffer.wrap(message));
    }

    public Cluster getCluster() {
        return cluster;
    }

    public ClusterChild getChild() {
        return child;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public WebSocket getWebSocket() {
        return webSocket;
    }

    public WebSocketListener getWebSocketListener() {
        return webSocketListener;
    }

    public WebSocketClient getCounterpart() {
        return counterpart;
    }

    public Scheme getScheme() {
        return scheme;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    public String getUrl() {
        return url;
    }

}
