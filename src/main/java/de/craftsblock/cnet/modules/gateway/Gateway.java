package de.craftsblock.cnet.modules.gateway;

import de.craftsblock.cnet.modules.gateway.entities.Cluster;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.Addon;
import de.craftsblock.craftsnet.addon.meta.annotations.Meta;
import de.craftsblock.craftsnet.api.utils.Scheme;
import de.craftsblock.craftsnet.builder.ActivateType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;

@Meta(name = "CNetGateway")
public class Gateway extends Addon {

    private final ConcurrentLinkedQueue<Cluster> clusters = new ConcurrentLinkedQueue<>();

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
        if (this.clusters.isEmpty())
            this.craftsNet().getBuilder().withSkipDefaultRoute(true);

        Cluster cluster = new Cluster(this, base, domain);
        this.clusters.add(cluster);
        return cluster;
    }

    public @Unmodifiable @NotNull Collection<Cluster> getClusters() {
        return Collections.unmodifiableCollection(clusters);
    }

}
