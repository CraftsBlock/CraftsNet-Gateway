package de.craftsblock.cnet.modules.gateway.proxy.websocket;

import de.craftsblock.craftsnet.api.websocket.WebSocketClient;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

public record WebSocketListener(WebSocketProxyClient parent, WebSocketClient counterpart) implements WebSocket.Listener {

    @Override
    public void onOpen(WebSocket webSocket) {
        WebSocket.Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        counterpart.sendMessage(data.toString());
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
        counterpart.sendMessage(data.array());
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
        counterpart.sendPing(message.array());
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
        counterpart.sendPong(message.array());
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        counterpart.close(statusCode, reason);
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        throw new RuntimeException(error);
    }

    @Override
    public WebSocketProxyClient parent() {
        return parent;
    }

    @Override
    public WebSocketClient counterpart() {
        return counterpart;
    }

}
