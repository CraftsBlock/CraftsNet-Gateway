package de.craftsblock.cnet.modules.gateway;

import de.craftsblock.cnet.modules.gateway.entities.Cluster;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.Addon;
import de.craftsblock.craftsnet.addon.meta.annotations.Meta;
import de.craftsblock.craftsnet.builder.ActivateType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

@Meta(name = "CNetGateway")
public class Gateway extends Addon {

    private final ConcurrentHashMap<Long, Cluster> clusters = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        CraftsNet.create(Gateway.class)
                .withArgs(args)
                .withWebServer(ActivateType.ENABLED)
                .withWebSocketServer(ActivateType.ENABLED)
                .build();
    }

    @Override
    public void onEnable() {

    public @NotNull Cluster createCluster(@NotNull String base) {
        return this.createCluster(base, ".*");
    }

    public @NotNull Cluster createCluster(@NotNull String base, @NotNull String domain) {
        Cluster cluster = new Cluster(base, domain);
        this.clusters.put(cluster.getIdLong(), cluster);
        return cluster;
    }

    public @Unmodifiable @NotNull Collection<Cluster> getClusters() {
        return Collections.unmodifiableCollection(clusters.values());
    }

}
