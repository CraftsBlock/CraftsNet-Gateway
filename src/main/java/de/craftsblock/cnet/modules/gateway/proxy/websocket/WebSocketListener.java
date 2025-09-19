package de.craftsblock.cnet.modules.gateway.proxy.websocket;

import de.craftsblock.craftsnet.api.websocket.WebSocketClient;

import java.net.http.WebSocket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

public record WebSocketListener(WebSocketProxyClient parent, WebSocketClient counterpart,
                                AtomicReference<ByteBuffer> binaryAccumulator,
                                AtomicReference<CharBuffer> textAccumulator) implements WebSocket.Listener {

    public WebSocketListener(WebSocketProxyClient parent, WebSocketClient counterpart) {
        this(parent, counterpart, new AtomicReference<>(), new AtomicReference<>());
        clearBinaryAccumulator();
        clearTextAccumulator();
    }

    /**
     * Clears and resets the binary accumulator buffer.
     */
    void clearTextAccumulator() {
        synchronized (textAccumulator) {
            textAccumulator.set(CharBuffer.allocate(8));
        }
    }

    /**
     * Clears and resets the binary accumulator buffer.
     */
    void clearBinaryAccumulator() {
        synchronized (binaryAccumulator) {
            binaryAccumulator.set(ByteBuffer.allocate(32));
        }
    }

    <T extends Buffer> T ensureCapacityAndGetAccumulator(final AtomicReference<T> buffer, int additionalCapacity,
                                                         Function<Integer, T> allocator, Consumer<T> updater) {
        T accumulator = buffer.get();
        if (accumulator.remaining() >= additionalCapacity)
            return accumulator;

        T expandedBuffer = allocator.apply(accumulator.capacity() + additionalCapacity);
        accumulator.flip();
        updater.accept(expandedBuffer);

        return expandedBuffer;
    }

    /**
     * Ensures the text accumulator buffer has enough capacity for additional data
     * and returns it.
     *
     * @param additionalCapacity The number of bytes to accommodate.
     * @return The buffer ready for writing additional data.
     */
    CharBuffer ensureCapacityAndGetTextAccumulator(int additionalCapacity) {
        synchronized (textAccumulator) {
            return ensureCapacityAndGetAccumulator(textAccumulator, additionalCapacity, CharBuffer::allocate, textAccumulator::set);
        }
    }

    /**
     * Ensures the binary accumulator buffer has enough capacity for additional data
     * and returns it.
     *
     * @param additionalCapacity The number of bytes to accommodate.
     * @return The buffer ready for writing additional data.
     */
    ByteBuffer ensureCapacityAndGetBinaryAccumulator(int additionalCapacity) {
        synchronized (binaryAccumulator) {
            return ensureCapacityAndGetAccumulator(binaryAccumulator, additionalCapacity, ByteBuffer::allocate, binaryAccumulator::set);
        }
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        clearBinaryAccumulator();
        clearTextAccumulator();
        WebSocket.Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        CharBuffer accumulator = ensureCapacityAndGetTextAccumulator(data.length());
        accumulator.put(data.toString());

        if (!last) return WebSocket.Listener.super.onText(webSocket, data, false);

        accumulator.flip();
        char[] accumulated = new char[accumulator.remaining()];
        accumulator.get(accumulated);

        try {
            counterpart.sendMessage(new String(accumulated));
            return WebSocket.Listener.super.onText(webSocket, data, true);
        } finally {
            clearTextAccumulator();
        }
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer message, boolean last) {
        byte[] data = new byte[message.remaining()];
        message.get(data);

        ByteBuffer accumulator = ensureCapacityAndGetBinaryAccumulator(data.length);
        accumulator.put(data);

        if (!last) return WebSocket.Listener.super.onBinary(webSocket, message, false);

        accumulator.flip();
        byte[] accumulated = new byte[accumulator.remaining()];
        accumulator.get(accumulated);

        try {
            counterpart.sendMessage(accumulated);
            return WebSocket.Listener.super.onBinary(webSocket, message, true);
        } finally {
            clearBinaryAccumulator();
        }
    }

    @Override
    public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
        counterpart.sendPing(message.array());
        return WebSocket.Listener.super.onPing(webSocket, message);
    }

    @Override
    public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
        counterpart.sendPong(message.array());
        return WebSocket.Listener.super.onPong(webSocket, message);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        counterpart.close(statusCode, reason);
        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        throw new RuntimeException("There was an unexpected error in the connection!", error);
    }

}
