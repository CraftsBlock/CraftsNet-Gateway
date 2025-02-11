package de.craftsblock.cnet.modules.gateway.entities;

import de.craftsblock.cnet.modules.gateway.Gateway;
import de.craftsblock.craftsnet.api.utils.Scheme;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Cluster extends Entity {

    private final @NotNull String base;
    private final @NotNull String domain;

    private final @NotNull Matcher baseMatcher;
    private final @NotNull Matcher domainMatcher;

    private final ConcurrentLinkedDeque<ClusterChild> http = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<ClusterChild> websocket = new ConcurrentLinkedDeque<>();

    public Cluster(@NotNull Gateway gateway, @NotNull String base, @NotNull String domain) {
        super(gateway);

        this.base = base;
        this.domain = domain;

        this.baseMatcher = Pattern.compile(sanitizeBasePattern(base)).matcher("");
        this.domainMatcher = Pattern.compile(domain).matcher("");
    }

    public ClusterChild createChild(@NotNull Scheme scheme, @NotNull String host, int port) {
        return this.createChild(scheme, host, port, "/");
    }

    public ClusterChild createChild(@NotNull Scheme scheme, @NotNull String host, int port, @NotNull String base) {
        ClusterChild child = new ClusterChild(this, scheme, host, port, base);
        this.add(child);
        return child;
    }

    public Cluster removeChild(long id) {
        getChildren().stream().filter(child -> child.getIdLong() == id).forEach(this::remove);
        return this;
    }

    public Cluster removeChild(ClusterChild child) {
        this.remove(child);
        return this;
    }

    private void add(ClusterChild child) {
        switch (child.getScheme()) {
            case HTTP, HTTPS -> http.addLast(child);
            case WS, WSS -> websocket.addLast(child);
        }
    }

    private void remove(ClusterChild child) {
        switch (child.getScheme()) {
            case HTTP, HTTPS -> http.remove(child);
            case WS, WSS -> websocket.remove(child);
        }
    }

    public ClusterChild getChild(Scheme scheme) {
        var children = switch (scheme) {
            case HTTP, HTTPS -> http;
            case WS, WSS -> websocket;
        };

        if (children.isEmpty()) return null;

        ClusterChild child = children.removeFirst();
        children.addLast(child);
        return child;
    }

    public boolean hasChild(Scheme scheme) {
        var children = switch (scheme) {
            case HTTP, HTTPS -> http;
            case WS, WSS -> websocket;
        };
        return !children.isEmpty();
    }

    public String removeBaseFromPath(String path) {
        Matcher matcher = this.baseMatcher.reset(path);
        if (!matcher.matches()) return null;
        return matcher.group("path");
    }

    public boolean isBaseApplicable(String path) {
        System.out.println(path + " matching to " + this.baseMatcher.pattern().pattern());
        return this.baseMatcher.reset(path).matches();
    }

    public @NotNull String getBase() {
        return this.base;
    }

    public boolean isDomainApplicable(String domain) {
        return this.domainMatcher.reset(domain).matches();
    }

    public @NotNull String getDomain() {
        return this.domain;
    }

    public @NotNull @Unmodifiable Collection<ClusterChild> getHttpChildren() {
        return Collections.unmodifiableCollection(http);
    }

    public @NotNull @Unmodifiable Collection<ClusterChild> getWebsocketChildren() {
        return Collections.unmodifiableCollection(websocket);
    }

    public @NotNull @Unmodifiable Collection<ClusterChild> getChildren() {
        return Stream.concat(http.stream(), websocket.stream()).toList();
    }

    private String sanitizeBasePattern(final String base) {
        final String pattern = base.trim();
        final StringBuilder builder = new StringBuilder(pattern.replaceFirst("(\\s+)?\\^/|(\\s+)?\\^|(\\s+)?/", ""));

        builder.insert(0, "^/");
        if (pattern.endsWith("$")) builder.deleteCharAt(builder.length() - 1);
        if (pattern.endsWith("/")) builder.deleteCharAt(builder.length() - 1);

        return builder + "(?<path>.*)$";
    }

}
