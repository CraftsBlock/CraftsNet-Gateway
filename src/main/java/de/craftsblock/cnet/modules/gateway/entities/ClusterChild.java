package de.craftsblock.cnet.modules.gateway.entities;

import de.craftsblock.craftsnet.api.utils.Scheme;
import de.craftsblock.craftsnet.api.websocket.WebSocketClient;
import de.craftsblock.craftsnet.utils.ByteBuffer;
import org.jetbrains.annotations.NotNull;

public class ClusterChild extends Entity {

    private final Cluster cluster;

    private final Scheme scheme;
    private final String host;
    private final int port;
    private final String base;

    public ClusterChild(@NotNull Cluster cluster, @NotNull Scheme scheme, @NotNull String host, int port, @NotNull String base) {
        this.cluster = cluster;

        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.base = base;
    }

    public ClusterChild(@NotNull Cluster cluster, @NotNull ByteBuffer buffer) {
        super(buffer);
        this.cluster = cluster;

        this.scheme = buffer.readEnum(Scheme.class);
        this.host = buffer.readUTF();
        this.port = buffer.readInt();
        this.base = buffer.readUTF();
    }

    public @NotNull String wrapBase(String path) {
        return (this.base + (!path.endsWith("/") ? "/" : "") + path).replaceAll("//+", "/");
    }

    @Override
    public void write(@NotNull ByteBuffer buffer) {
        super.write(buffer);

        buffer.writeEnum(this.scheme);
        buffer.writeUTF(this.host);
        buffer.writeInt(this.port);
        buffer.writeUTF(this.base);
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

}
