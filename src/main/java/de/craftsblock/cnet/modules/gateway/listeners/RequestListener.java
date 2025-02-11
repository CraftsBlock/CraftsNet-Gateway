package de.craftsblock.cnet.modules.gateway.listeners;

import de.craftsblock.cnet.modules.gateway.Gateway;
import de.craftsblock.cnet.modules.gateway.entities.Cluster;
import de.craftsblock.cnet.modules.gateway.entities.ClusterChild;
import de.craftsblock.craftscore.event.EventHandler;
import de.craftsblock.craftscore.event.ListenerAdapter;
import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftsnet.api.http.Exchange;
import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.http.Response;
import de.craftsblock.craftsnet.autoregister.meta.AutoRegister;
import de.craftsblock.craftsnet.events.requests.PreRequestEvent;

import java.io.IOException;

@AutoRegister
public class RequestListener implements ListenerAdapter {

    private final Gateway gateway;

    public RequestListener(Gateway gateway) {
        this.gateway = gateway;
    }

    @EventHandler
    public void handlePreRequest(PreRequestEvent event) throws IOException, InterruptedException {
        Exchange exchange = event.getExchange();
        Response response = exchange.response();
        Request request = exchange.request();
        if (gateway.routeRegistry().hasRouteMappings(exchange.request())) return;

        Cluster cluster = gateway.getMatchingCluster(request.getUrl(), request.getDomain(), exchange.scheme());
        if (cluster == null) return;
        event.setCancelled(true);

        gateway.logger().info(request.getHttpMethod().toString() + " " + request.getUrl() + " from " + request.getIp() + " \u001b[38;5;50m[PROXIED]");

        if (!cluster.hasChild(exchange.scheme())) {
            response.print(Json.empty()
                    .set("message", "Cluster has no children")
                    .set("status", "404"));
            return;
        }

        ClusterChild child = cluster.getChild(exchange.scheme());
        child.newHttpProxyClient().proxyRequest(exchange);
    }


}
