package com.freenote.app.server.core;

import com.freenote.app.server.core.v2.*;
import com.freenote.app.server.exceptions.SelectorInterruptException;
import com.freenote.app.server.model.LegacyIOWrapper;
import com.freenote.app.server.socket.RawSocket;
import com.freenote.app.server.socket.ServerSocketFactory;
import lombok.AllArgsConstructor;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import otel.GlobalOpenTelemetryManualInstrumentationUsage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.freenote.app.server.util.RuntimeUtils.getAvailableProcessors;
import static com.freenote.app.server.util.RuntimeUtils.logServerInitialization;

@AllArgsConstructor
public class ServerBootstrap {
    private ExecutorService executorService = Executors.newFixedThreadPool(getAvailableProcessors());


    private static final Logger log = LogManager.getLogger(ServerBootstrap.class);
    private ServerSocketFactory serverSocketFactory = new RawSocket();
    @Setter
    private int port = 8189;
    private GlobalOpenTelemetryManualInstrumentationUsage sampleTelemetry = new GlobalOpenTelemetryManualInstrumentationUsage();

    public ServerBootstrap(ServerSocketFactory serverSocketFactory) {
        this.serverSocketFactory = serverSocketFactory;
        initTelemetry();
    }

    public ServerBootstrap() {
        initTelemetry();
    }

    private void initTelemetry() {
        sampleTelemetry.globalOpenTelemetryUsage();
        sampleTelemetry.providersUsage();
    }

    public void start(LegacyIncomingConnectionHandler handler) throws Exception {
        logServerInitialization();
        try (var serverSocket = serverSocketFactory.createServerSocket(this.port)) {
            while (!serverSocket.isClosed()) {
                log.info("Waiting for connection on port {}", this.port);
                var socket = serverSocket.accept(); // block method
                log.info("Accepted connection from {}", socket.getRemoteSocketAddress());
                this.executorService.submit(() -> {
                    try {
                        handler.handle(new LegacyIOWrapper(socket));
                    } catch (Exception e) {
                        log.error("Error handling connection", e);
                    }
                });
            }
        }
    }


    public void start(IncomingConnectionHandlerV2 handler) throws Exception {
        var selector = openSelector();
        try (var serverSocketChannel = tryOpenSocketChannel()) {
            registerAcceptEvent(serverSocketChannel, selector);
            logServerInitialization();
            startBusyWaitingSelector(handler, selector);
        }

    }

    private Selector openSelector() throws IOException {
        return Selector.open();
    }

    private void startBusyWaitingSelector(IncomingConnectionHandlerV2 handler, Selector selector) throws ExecutionException, InterruptedException {
        Future blockChannel = this.executorService.submit(() -> {
            try {
                startThreadSelector(selector, handler);
            } catch (IOException e) {
                log.info("Error during runtime of thread selector", e);
                throw new SelectorInterruptException("Thread for readiness selection are interrupted", e);
            }
        });
        blockChannel.get();
    }

    private void registerAcceptEvent(ServerSocketChannel serverSocketChannel, Selector selector) throws ClosedChannelException {
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    private ServerSocketChannel tryOpenSocketChannel() throws IOException {
        var serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(this.port));
        log.info("Starting server on port {}", this.port);
        return serverSocketChannel;
    }

    private void startThreadSelector(Selector selector, IncomingConnectionHandlerV2 handler) throws IOException {
        while (selector.isOpen()) {
            int numReadyChannels = selector.select();
            if (numReadyChannels == 0) continue;

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                handleSelectedKey(selector, handler, key);
                keyIterator.remove();
            }
        }
    }

    private void handleSelectedKey(Selector selector, IncomingConnectionHandlerV2 handler, SelectionKey key) throws IOException {
        if (key.isAcceptable()) {
            handleNewConnectionEvent(selector, (ServerSocketChannel) key.channel());
        } else if (key.isReadable()) {
            handleReadableEvent(handler, key);
        }
    }

    private void handleNewConnectionEvent(Selector selector, ServerSocketChannel server) throws IOException {
        SocketChannel client = server.accept();
        if (client != null) {
            client.configureBlocking(false);
            ConnectionState state = new HandShakeState();
            client.register(selector, SelectionKey.OP_READ, state);
        }
    }

    private void handleReadableEvent(IncomingConnectionHandlerV2 handler, SelectionKey key) throws IOException {
        ConnectionState state = (ConnectionState) key.attachment();
        var tracingContext = buildTraceContext(state);

        try {
            state.handle(
                    handler,
                    ReadableContext.builder()
                            .tracingContext(tracingContext)
                            .channel((SocketChannel) key.channel())
                            .key(key)
                            .build()
            );
        } finally {
            if (tracingContext != null && tracingContext.getSpan() != null) {
                tracingContext.getSpan().end();
            }
        }
    }

    private TracingContext buildTraceContext(ConnectionState state) {
        String spanName = state instanceof HandShakeState ? "WebSocket.Handshake" : "WebSocket.Message";
        var span = sampleTelemetry.getTracer().spanBuilder(spanName).startSpan();
        return TracingContext.builder()
                .span(span)
                .build();
    }

}
