package com.freenote.app.server.core.nio.events;

import com.freenote.app.server.core.context.ConnectionContext;
import com.freenote.app.server.core.context.ReadableContext;
import com.freenote.app.server.core.nio.state.ConnectionState;
import com.freenote.app.server.core.nio.state.HandShakeState;
import com.freenote.app.server.core.nio.state.MessageState;
import com.freenote.app.server.core.nio.transport.NetworkSelector;
import com.freenote.app.server.exceptions.AcceptConnectionException;
import com.freenote.app.server.model.ws.NIONetworkRequestData;
import io.opentelemetry.api.trace.Span;
import lombok.Builder;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import otel.metrics.MetricUtils;
import otel.sdk.context.TracingContext;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static otel.SampleGlobalOpenTelemetry.getSampleGlobalTelemetry;

@Builder
@Getter
public class NIOServerSession {
    private NetworkSelector selector;
    private ServerSocketChannel serverSocketChannel;
    private SocketChannel socketChannel;
    private static final Logger log = LogManager.getLogger(NIOServerSession.class);
    private final Map<SocketChannel, ConnectionState> channelStates = new ConcurrentHashMap<>();
    private final Map<SocketChannel, ByteBuffer> channelBuffers = new ConcurrentHashMap<>();

    public void registerAcceptEvent() {
        try {
            serverSocketChannel.register(selector.getSelector(), SelectionKey.OP_ACCEPT);
        } catch (ClosedChannelException e) {
            throw new AcceptConnectionException("Failed to register accept event for server socket channel", e);
        }

    }

    public void acceptConnection(NIOEvent nioEvent) {
        SocketChannel client = null;
        try {
            client = serverSocketChannel.accept();
            if (client != null) {
                client.configureBlocking(false);
                channelStates.put(client, new HandShakeState());
                channelBuffers.put(client, ByteBuffer.allocateDirect(2048));
                client.register(selector.getSelector(), SelectionKey.OP_READ);
            }
        } catch (IOException e) {
            throw new AcceptConnectionException("Failed to accept connection", e);
        }
    }

    public void handleReadEvent(NIOEvent nioEvent) {
        try {
            SocketChannel channel = (SocketChannel) nioEvent.getChannel();
            ConnectionState state = channelStates.get(channel);
            ByteBuffer buffer = channelBuffers.get(channel);

            if (state == null || buffer == null) {
                log.warn("No state or buffer for channel {}", channel);
                return;
            }

            NIONetworkRequestData networkData = new NIONetworkRequestData(channel, buffer);

            if (!readChannelData(nioEvent, networkData)) {
                return;
            }

            var span = buildStartSpan(state);
            TracingContext tracingContext = TracingContext.builder()
                    .span(span)
                    .build();
            ReadableContext readableContext = ReadableContext.builder()
                    .tracingContext(tracingContext)
                    .httpUpgradeRequest(state instanceof MessageState ps ? ps.getRequest() : null)
                    .build();

            ConnectionContext context = ConnectionContext.builder()
                    .networkRequestData(networkData)
                    .readableContext(readableContext)
                    .build();

            ConnectionState nextState = state.handle(handler, context);
            if (nextState == null) {
                channelStates.remove(channel);
                channelBuffers.remove(channel);
                key.cancel();
                context.getNetworkRequestData().close();
            } else if (nextState != state) {
                channelStates.put(channel, nextState);
            }
            if (tracingContext.getSpan() != null) {
                tracingContext.getSpan().end();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private boolean readChannelData(NIOEvent nioEvent, NIONetworkRequestData networkData) {
        try {
            if (networkData.readFromChannel() == -1) {
                cleanupChannel(nioEvent, networkData);
                return false;
            }
            networkData.prepareForRead();
            return true;
        } catch (IOException e) {
            log.warn("I/O error reading from channel {}", e.getMessage());
            cleanupChannel(nioEvent, networkData);
            return false;
        }
    }


    private Span buildStartSpan(ConnectionState state) {
        String spanName = state instanceof HandShakeState ? "websocket.handshake" : "websocket.message";
        return getSampleGlobalTelemetry().getTracer().spanBuilder(spanName)
                .setAttribute("server.address", "localhost")
                .setAttribute("server.port", -1)
                .setAttribute("network.transport", "tcp")
                .setAttribute("app.websocket.state", state.getClass().getSimpleName())
                .startSpan();
    }


    private void cleanupChannel(NIOEvent nioEvent, NIONetworkRequestData networkData) {
        channelStates.remove(channel);
        channelBuffers.remove(channel);
        key.cancel();
        try {
            networkData.close();
        } catch (IOException ignored) {
        }
        MetricUtils.decrementConcurrentUsers();
    }
}
