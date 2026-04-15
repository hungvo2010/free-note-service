package com.freenote.app.server.core.startup;

import com.freenote.app.server.core.connection.WebSocketSession;
import com.freenote.app.server.core.connection.state.ConnectionState;
import com.freenote.app.server.core.connection.state.HandShakeState;
import com.freenote.app.server.core.context.ReadableContext;
import com.freenote.app.server.core.context.TracingContext;
import com.freenote.app.server.core.legacy.LegacyIncomingConnectionHandler;
import com.freenote.app.server.core.nio.ModernIncomingConnectionHandler;
import com.freenote.app.server.core.transport.NetworkSelector;
import com.freenote.app.server.exceptions.SelectorInterruptException;
import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.model.OutputWrapper;
import com.freenote.app.server.socket.RawSocket;
import com.freenote.app.server.socket.ServerSocketFactory;
import lombok.AllArgsConstructor;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import otel.metrics.MetricUtils;

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
import static otel.SampleGlobalOpenTelemetry.getSampleGlobalTelemetry;

@AllArgsConstructor
public class ServerBootstrap {
    private ExecutorService executorService = Executors.newFixedThreadPool(getAvailableProcessors());
    private ExecutorService virtualExecutorService = Executors.newVirtualThreadPerTaskExecutor();


    private static final Logger log = LogManager.getLogger(ServerBootstrap.class);
    private ServerSocketFactory serverSocketFactory = new RawSocket();
    @Setter
    private int port = 8189;

    public ServerBootstrap(ServerSocketFactory serverSocketFactory) {
        this.serverSocketFactory = serverSocketFactory;
    }

    public ServerBootstrap() {
    }

    public void start(LegacyIncomingConnectionHandler handler) throws Exception {
        logServerInitialization();
        Thread t = Thread.ofVirtual()
                .name("my-worker")
                .unstarted(() -> {
                    log.warn("Running in virtual thread: {}, Is Virtual: {}", Thread.currentThread(), Thread.currentThread().isVirtual());
                });

        t.start();
        t.join();
        try (var serverSocket = serverSocketFactory.createServerSocket(this.port)) {
            while (!serverSocket.isClosed()) {
                log.info("Waiting for connection on port {}", this.port);
                var socket = serverSocket.accept(); // block method
                log.info("Accepted connection from {}", socket.getRemoteSocketAddress());
                this.virtualExecutorService.submit(() -> {
                    try {
                        var session = WebSocketSession.builder()
                                .socket(socket)
                                .inputWrapper(new InputWrapper(socket))
                                .outputWrapper(new OutputWrapper(socket.getOutputStream()))
                                .build();
                        handler.handle(session);
                    } catch (Exception e) {
                        log.error("Error handling connection", e);
                    }
                });
            }
        }
    }


    public void start(ModernIncomingConnectionHandler handler) throws Exception {
        var selector = openNetworkSelector();
        try (var serverSocketChannel = tryOpenSocketChannel()) {
            registerAcceptEvent(serverSocketChannel, selector);
            logServerInitialization();
            startBusyWaitingSelector(handler, selector);
        }

    }

    private NetworkSelector openNetworkSelector() throws IOException {
        return new NetworkSelector(Selector.open());
    }

    private void startBusyWaitingSelector(ModernIncomingConnectionHandler handler, NetworkSelector selector) throws ExecutionException, InterruptedException {
        Future blockChannel = this.virtualExecutorService.submit(() -> {
            startSingleNetworkSelector(handler, selector);
        });
        blockChannel.get();
    }

    private void startSingleNetworkSelector(ModernIncomingConnectionHandler handler, NetworkSelector selector) {
        try {
            runSelectorLoop(selector, handler);
        } catch (IOException e) {
            log.info("Error during runtime of thread selector", e);
            throw new SelectorInterruptException("Thread for readiness selection are interrupted", e);
        }
    }

    private void registerAcceptEvent(ServerSocketChannel serverSocketChannel, NetworkSelector networkSelector) throws ClosedChannelException {
        serverSocketChannel.register(networkSelector.getSelector(), SelectionKey.OP_ACCEPT);
    }

    private ServerSocketChannel tryOpenSocketChannel() throws IOException {
        var serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(this.port));
        log.info("Starting server on port {}", this.port);
        return serverSocketChannel;
    }

    private void runSelectorLoop(NetworkSelector selector, ModernIncomingConnectionHandler handler) throws IOException {
        while (selector.isHealthy()) {
            waitForEvents(selector);
            dispatcherReadyEvents(selector, handler);
        }
    }

    private void dispatcherReadyEvents(NetworkSelector selector, ModernIncomingConnectionHandler handler) throws IOException {
        Set<SelectionKey> selectedKeys = selector.getNewSelectionEvents();
        Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
        while (keyIterator.hasNext()) {
            SelectionKey key = keyIterator.next();
            handleSelectedKey(selector, handler, key);
            keyIterator.remove();
        }
    }

    private void waitForEvents(NetworkSelector selector) throws IOException {
        int numReadyChannels = selector.select();
        if (numReadyChannels == 0) throw new SelectorInterruptException("Selector is interrupted or no channels are ready");
    }

    private void handleSelectedKey(NetworkSelector selector, ModernIncomingConnectionHandler handler, SelectionKey key) throws IOException {
        if (key.isAcceptable()) {
            handleNewConnectionEvent(selector, (ServerSocketChannel) key.channel());
        } else if (key.isReadable()) {
            handleReadableEvent(handler, key);
        }
    }

    private void handleNewConnectionEvent(NetworkSelector selector, ServerSocketChannel server) throws IOException {
        SocketChannel client = server.accept();
        if (client != null) {
            client.configureBlocking(false);
            ConnectionState state = new HandShakeState();
            client.register(selector.getSelector(), SelectionKey.OP_READ, state);
            MetricUtils.incrementConcurrentUsers();
        }
    }

    private void handleReadableEvent(ModernIncomingConnectionHandler handler, SelectionKey key) throws IOException {
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
        String spanName = state instanceof HandShakeState ? "websocket.handshake" : "websocket.message";
        var span = getSampleGlobalTelemetry().getTracer().spanBuilder(spanName)
                .setAttribute("server.address", "localhost")
                .setAttribute("server.port", (long) this.port)
                .setAttribute("network.transport", "tcp")
                .setAttribute("app.websocket.state", state.getClass().getSimpleName())
                .startSpan();

        return TracingContext.builder()
                .span(span)
                .build();
    }

}
