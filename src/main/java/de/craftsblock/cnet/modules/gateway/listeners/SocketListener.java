package de.craftsblock.cnet.modules.gateway.listeners;

import de.craftsblock.cnet.modules.gateway.Gateway;
import de.craftsblock.cnet.modules.gateway.entities.Cluster;
import de.craftsblock.cnet.modules.gateway.proxy.websocket.WebSocketProxyClient;
import de.craftsblock.craftscore.event.EventHandler;
import de.craftsblock.craftscore.event.ListenerAdapter;
import de.craftsblock.craftsnet.api.websocket.Frame;
import de.craftsblock.craftsnet.api.websocket.SocketExchange;
import de.craftsblock.craftsnet.api.websocket.WebSocketClient;
import de.craftsblock.craftsnet.autoregister.meta.AutoRegister;
import de.craftsblock.craftsnet.events.sockets.ClientConnectEvent;
import de.craftsblock.craftsnet.events.sockets.ClientDisconnectEvent;
import de.craftsblock.craftsnet.events.sockets.message.IncomingSocketMessageEvent;
import de.craftsblock.craftsnet.events.sockets.message.ReceivedPingMessageEvent;
import de.craftsblock.craftsnet.events.sockets.message.ReceivedPongMessageEvent;

@AutoRegister
public class SocketListener implements ListenerAdapter {

    private final Gateway gateway;

    public SocketListener(Gateway gateway) {
        this.gateway = gateway;
    }

    @EventHandler
    public void handleConnect(ClientConnectEvent event) {
        if (event.isCancelled()) return;

        SocketExchange exchange = event.getExchange();
        WebSocketClient client = exchange.client();
        boolean proxyCandidate = !event.hasMappings();

        try {
            if (!proxyCandidate) return;

            Cluster cluster = gateway.getMatchingCluster(client.getPath(), client.getDomain(), exchange.scheme());
            if (cluster == null) return;

            event.allowWithoutMapping(true);
            WebSocketProxyClient proxyClient = cluster.getChild(exchange.scheme()).newWSProxyClient(client, client.getPath());
            client.getSession().put("proxied.counterpart", proxyClient);
        } finally {
            client.getSession().put("proxied.candidate", proxyCandidate);
        }
    }

    @EventHandler
    public void handleDisconnect(ClientDisconnectEvent event) {
        SocketExchange exchange = event.getExchange();
        WebSocketClient client = exchange.client();
        if (!isProxyCandidate(client)) return;

        WebSocketProxyClient counterpart = client.getSession().getAsType("proxied.counterpart", WebSocketProxyClient.class);
        if (event.getRawCloseCode() > 0) counterpart.close(event.getRawCloseCode(), event.getCloseReason());
        else if (counterpart != null) counterpart.kill();
    }

    @EventHandler
    public void handleIncoming(IncomingSocketMessageEvent event) {
        SocketExchange exchange = event.getExchange();
        WebSocketClient client = exchange.client();
        if (!isProxyCandidate(client)) return;

        WebSocketProxyClient counterpart = client.getSession().getAsType("proxied.counterpart", WebSocketProxyClient.class);
        Frame frame = event.getFrame();
        switch (frame.getOpcode()) {
            case TEXT -> counterpart.sendMessage(frame.getUtf8());
            case BINARY -> counterpart.sendMessage(frame.getData());
        }
    }

    @EventHandler
    public void handlePing(ReceivedPingMessageEvent event) {
        WebSocketClient client = event.getClient();
        if (!isProxyCandidate(client)) return;

        WebSocketProxyClient counterpart = client.getSession().getAsType("proxied.counterpart", WebSocketProxyClient.class);
        counterpart.sendPing(event.getMessage() != null ? event.getMessage() : new byte[0]);
    }

    @EventHandler
    public void handlePong(ReceivedPongMessageEvent event) {
        WebSocketClient client = event.getClient();
        if (!isProxyCandidate(client)) return;

        WebSocketProxyClient counterpart = client.getSession().getAsType("proxied.counterpart", WebSocketProxyClient.class);
        counterpart.sendPong(event.getMessage() != null ? event.getMessage() : new byte[0]);
    }

    private boolean isProxyCandidate(WebSocketClient client) {
        return client.getSession().getAsType("proxied.candidate", false, boolean.class);
    }

}
