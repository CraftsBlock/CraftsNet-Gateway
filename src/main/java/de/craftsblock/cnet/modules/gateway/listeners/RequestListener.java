package de.craftsblock.cnet.modules.gateway.listeners;

import de.craftsblock.cnet.modules.gateway.Gateway;
import de.craftsblock.cnet.modules.gateway.entities.Cluster;
import de.craftsblock.craftscore.event.EventHandler;
import de.craftsblock.craftscore.event.ListenerAdapter;
import de.craftsblock.craftsnet.api.http.Exchange;
import de.craftsblock.craftsnet.api.utils.Scheme;
import de.craftsblock.craftsnet.autoregister.meta.AutoRegister;
import de.craftsblock.craftsnet.events.requests.PreRequestEvent;

import java.io.IOException;

@AutoRegister
public class RequestListener implements ListenerAdapter {

    private final Cluster cluster;
    private final Gateway addon;

    public RequestListener(Gateway addon) {
        this.addon = addon;
        this.cluster = addon.createCluster("/")
                .registerChild(Scheme.HTTPS, "craftsblock.de", 443);
    }

    @EventHandler
    public void handlePreRequest(PreRequestEvent event) throws IOException, InterruptedException {
        Exchange exchange = event.getExchange();
        if (addon.routeRegistry().hasRouteMappings(exchange.request())) return;

        event.setCancelled(true);
    }


}
