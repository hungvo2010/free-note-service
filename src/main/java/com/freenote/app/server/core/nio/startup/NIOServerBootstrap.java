package com.freenote.app.server.core.nio.startup;

import com.freenote.app.server.core.config.ServerSocketConfig;
import com.freenote.app.server.core.connection.IncomingConnectionHandler;
import com.freenote.app.server.core.context.ConnectionContext;
import com.freenote.app.server.core.context.ReadableContext;
import com.freenote.app.server.core.context.TracingContext;
import com.freenote.app.server.core.nio.state.ConnectionState;
import com.freenote.app.server.core.nio.state.HandShakeState;
import com.freenote.app.server.core.nio.state.ProcessingState;
import com.freenote.app.server.core.nio.transport.NetworkSelector;
import com.freenote.app.server.core.startup.ServerBootstrap;
import com.freenote.app.server.exceptions.SelectorInterruptException;
import com.freenote.app.server.model.ws.NIONetworkRequestData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import otel.metrics.MetricUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;

import static com.freenote.app.server.util.RuntimeUtils.logServerInitialization;
import static otel.SampleGlobalOpenTelemetry.getSampleGlobalTelemetry;

public class NIOServerBootstrap implements ServerBootstrap {
    private static final Logger log = LogManager.getLogger(NIOServerBootstrap.class);
    private AbstractExecutorService virtualExecutorService;
    private IncomingConnectionHandler handler;

    private final Map<SocketChannel, ConnectionState> channelStates = new ConcurrentHashMap<>();
    private final Map<SocketChannel, ByteBuffer> channelBuffers = new ConcurrentHashMap<>();

    @Override
    public void start(IncomingConnectionHandler handler, ServerSocketConfig socketConfig) throws Exception {
        this.handler = handler;
        var selector = openNetworkSelector();
        try (var serverSocketChannel = tryOpenSocketChannel(socketConfig)) {
            registerAcceptEvent(serverSocketChannel, selector);
            logServerInitialization();
            startBusyWaitingSelector(selector);
        }
    }

    private void startBusyWaitingSelector(NetworkSelector selector) throws ExecutionException, InterruptedException {
        this.virtualExecutorService = Optional.ofNullable(this.virtualExecutorService).orElseGet(() -> (AbstractExecutorService) Executors.newFixedThreadPool(2));
        Future<?> blockChannel = this.virtualExecutorService.submit(() -> {
            startSingleNetworkSelector(selector);
        });
        blockChannel.get();
    }

    private void startSingleNetworkSelector(NetworkSelector selector) {
        try {
            runSelectorLoop(selector);
        } catch (IOException e) {
            log.info("Error during runtime of thread selector", e);
            throw new SelectorInterruptException("Thread for readiness selection are interrupted", e);
        }
    }

    private ServerSocketChannel tryOpenSocketChannel(ServerSocketConfig socketConfig) throws IOException {
        var serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(socketConfig.port()));
        log.info("Starting server on port {}", socketConfig.port());
        return serverSocketChannel;
    }

    private void runSelectorLoop(NetworkSelector selector) throws IOException {
        while (selector.isHealthy()) {
            log.info("NIO Selector is waiting for events...");
            waitForEvents(selector);
            dispatcherReadyEvents(selector);
        }
    }

    private void dispatcherReadyEvents(NetworkSelector selector) throws IOException {
        Set<SelectionKey> selectedKeys = selector.getNewSelectionEvents();
        Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
        while (keyIterator.hasNext()) {
            SelectionKey key = keyIterator.next();
            handleSelectedKey(selector, key);
            keyIterator.remove();
        }
    }

    private void registerAcceptEvent(ServerSocketChannel serverSocketChannel, NetworkSelector networkSelector) throws ClosedChannelException {
        serverSocketChannel.register(networkSelector.getSelector(), SelectionKey.OP_ACCEPT);
    }

    private void waitForEvents(NetworkSelector selector) throws IOException {
        int numReadyChannels = selector.select();
        if (numReadyChannels == 0)
            throw new SelectorInterruptException("Selector is interrupted or no channels are ready");
    }

    private void handleSelectedKey(NetworkSelector selector, SelectionKey key) throws IOException {
        if (key.isAcceptable()) {
            handleNewConnectionEvent(selector, (ServerSocketChannel) key.channel());
        } else if (key.isReadable()) {
            handleReadableEvent(key);
        }
    }

    private void handleNewConnectionEvent(NetworkSelector selector, ServerSocketChannel server) throws IOException {
        SocketChannel client = server.accept();
        if (client != null) {
            client.configureBlocking(false);
            channelStates.put(client, new HandShakeState());
            channelBuffers.put(client, ByteBuffer.allocateDirect(2048));
            client.register(selector.getSelector(), SelectionKey.OP_READ);
            MetricUtils.incrementConcurrentUsers();
        }
    }

    private void handleReadableEvent(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ConnectionState state = channelStates.get(channel);
        ByteBuffer buffer = channelBuffers.get(channel);

        if (state == null || buffer == null) {
            log.warn("No state or buffer for channel {}", channel);
            return;
        }

        NIONetworkRequestData networkData = new NIONetworkRequestData(channel, buffer);

        if (!readChannelData(channel, key, networkData)) {
            return;
        }

        String spanName = state instanceof HandShakeState ? "websocket.handshake" : "websocket.message";
        var span = getSampleGlobalTelemetry().getTracer().spanBuilder(spanName)
                .setAttribute("server.address", "localhost")
                .setAttribute("server.port", -1)
                .setAttribute("network.transport", "tcp")
                .setAttribute("app.websocket.state", state.getClass().getSimpleName())
                .startSpan();

        TracingContext tracingContext = TracingContext.builder()
                .span(span)
                .build();

        ReadableContext readableContext = ReadableContext.builder()
                .networkRequestData(networkData)
                .tracingContext(tracingContext)
                .httpUpgradeRequest(state instanceof ProcessingState ps ? ps.getRequest() : null)
                .build();

        ConnectionContext context = ConnectionContext.builder()
                .networkRequestData(networkData)
                .readableContext(readableContext)
                .build();

        try {
            ConnectionState nextState = state.handle(handler, context);
            if (nextState == null) {
                channelStates.remove(channel);
                channelBuffers.remove(channel);
                key.cancel();
                context.getNetworkRequestData().close();
            } else if (nextState != state) {
                channelStates.put(channel, nextState);
            }
        } finally {
            if (tracingContext.getSpan() != null) {
                tracingContext.getSpan().end();
            }
        }
    }

    private boolean readChannelData(SocketChannel channel, SelectionKey key,
                                     NIONetworkRequestData networkData) {
        try {
            if (networkData.readFromChannel() == -1) {
                cleanupChannel(channel, key, networkData);
                return false;
            }
            networkData.prepareForRead();
            return true;
        } catch (IOException e) {
            log.warn("I/O error reading from channel {}: {}", channel, e.getMessage());
            cleanupChannel(channel, key, networkData);
            return false;
        }
    }

    private void cleanupChannel(SocketChannel channel, SelectionKey key,
                                NIONetworkRequestData networkData) {
        channelStates.remove(channel);
        channelBuffers.remove(channel);
        key.cancel();
        try {
            networkData.close();
        } catch (IOException ignored) {
        }
        MetricUtils.decrementConcurrentUsers();
    }

    private NetworkSelector openNetworkSelector() throws IOException {
        return new NetworkSelector(Selector.open());
    }
}
