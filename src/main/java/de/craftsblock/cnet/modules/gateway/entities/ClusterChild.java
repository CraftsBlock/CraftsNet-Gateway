package de.craftsblock.cnet.modules.gateway.entities;

import de.craftsblock.cnet.modules.gateway.proxy.http.HttpProxyClient;
import de.craftsblock.cnet.modules.gateway.proxy.websocket.WebSocketProxyClient;
import de.craftsblock.craftsnet.api.utils.Scheme;
import de.craftsblock.craftsnet.api.websocket.WebSocketClient;
import org.jetbrains.annotations.NotNull;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class ClusterChild extends Entity {

    private final Cluster cluster;

    private final Scheme scheme;
    private final String host;
    private final int port;
    private final String base;

    private boolean httpCacheAllowed = true;
    private Duration httpConnectTimeout = Duration.of(2, ChronoUnit.SECONDS);
    private HttpClient.Redirect httpRedirectPolicy = HttpClient.Redirect.NORMAL;

    public ClusterChild(@NotNull Cluster cluster, @NotNull Scheme scheme, @NotNull String host, int port, @NotNull String base) {
        super(cluster.getGateway());
        this.cluster = cluster;

        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.base = ("/" + base).replaceAll("//+", "/");
    }

    public @NotNull String wrapBase(String path) {
        String trimmed = path.trim();
        if (trimmed.isBlank() || trimmed.equalsIgnoreCase("/")) return this.base;
        return (this.base + (!trimmed.endsWith("/") ? "/" : "") + trimmed).replaceAll("//+", "/");
    }

    public @NotNull HttpProxyClient newHttpProxyClient() {
        return new HttpProxyClient(this.cluster, this);
    }

    public @NotNull WebSocketProxyClient newWSProxyClient(@NotNull WebSocketClient counterpart, @NotNull String path) {
        return new WebSocketProxyClient(counterpart, this.scheme, this.host, this.port, this.wrapBase(path));
    }

    public @NotNull Cluster getCluster() {
        return cluster;
    }

    public @NotNull Scheme getScheme() {
        return scheme;
    }

    public @NotNull String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public @NotNull String getBase() {
        return base;
    }

    public ClusterChild setHttpCacheAllowed(boolean httpCacheAllowed) {
        this.httpCacheAllowed = httpCacheAllowed;
        return this;
    }

    public boolean isHttpCacheAllowed() {
        return httpCacheAllowed;
    }

    public ClusterChild setHttpConnectTimeout(Duration httpConnectTimeout) {
        this.httpConnectTimeout = httpConnectTimeout;
        return this;
    }

    public Duration getHttpConnectTimeout() {
        return httpConnectTimeout;
    }

    public ClusterChild setHttpRedirectPolicy(HttpClient.Redirect httpRedirectPolicy) {
        this.httpRedirectPolicy = httpRedirectPolicy;
        return this;
    }

    public HttpClient.Redirect getHttpRedirectPolicy() {
        return httpRedirectPolicy;
    }

}
