package com.freenote.app.server.core.nio.startup;

import com.freenote.app.server.core.config.ServerSocketConfig;
import com.freenote.app.server.core.connection.IncomingConnectionHandler;
import com.freenote.app.server.core.nio.events.NIOEvent;
import com.freenote.app.server.core.nio.events.NIOServerSession;
import com.freenote.app.server.core.nio.transport.NetworkSelector;
import com.freenote.app.server.core.startup.ServerBootstrap;
import com.freenote.app.server.exceptions.SelectorInterruptException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import otel.metrics.MetricUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.freenote.app.server.util.RuntimeUtils.logServerInitialization;

public class NIOServerBootstrap implements ServerBootstrap {
    private static final Logger log = LogManager.getLogger(NIOServerBootstrap.class);
    private AbstractExecutorService virtualExecutorService;
    private IncomingConnectionHandler handler;


    @Override
    public void start(IncomingConnectionHandler handler, ServerSocketConfig socketConfig) throws Exception {
        this.handler = handler;
        var selector = openNetworkSelector();
        try (var serverSocketChannel = tryOpenSocketChannel(socketConfig)) {
            var nioServerSession = NIOServerSession.builder()
                    .serverSocketChannel(serverSocketChannel)
                    .selector(selector)
                    .build();
            nioServerSession.registerAcceptEvent();
            logServerInitialization();
            startBusyWaitingSelector(nioServerSession);
        }
    }

    private void startBusyWaitingSelector(NIOServerSession nioServerSession) throws ExecutionException, InterruptedException {
        this.virtualExecutorService = (AbstractExecutorService) Executors.newFixedThreadPool(2);
        Future<?> blockChannel = this.virtualExecutorService.submit(() -> {
            runSelectorLoop(nioServerSession);
        });
        blockChannel.get();
    }

    private ServerSocketChannel tryOpenSocketChannel(ServerSocketConfig socketConfig) throws IOException {
        var serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(socketConfig.port()));
        log.info("Starting server on port {}", socketConfig.port());
        return serverSocketChannel;
    }

    private void runSelectorLoop(NIOServerSession nioServerSession) {
        try {
            var selector = nioServerSession.getSelector();
            while (selector.isHealthy()) {
                log.info("NIO Selector is waiting for events...");
                waitForEvents(selector);
                dispatcherReadyEvents(nioServerSession);
            }
        } catch (IOException e) {
            log.error("Error during NIO selector loop", e);
        }
    }

    private void dispatcherReadyEvents(NIOServerSession nioServerSession) throws IOException {
        var selector = nioServerSession.getSelector();
        Set<SelectionKey> selectedKeys = selector.getNewSelectionEvents();
        Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
        while (keyIterator.hasNext()) {
            SelectionKey key = keyIterator.next();
            var nioEvent = NIOEvent.builder().selectionKey(key).build();
            handleSelectedKey(nioServerSession, nioEvent);
            keyIterator.remove();
        }
    }

    private void waitForEvents(NetworkSelector selector) throws IOException {
        int numReadyChannels = selector.select();
        if (numReadyChannels == 0)
            throw new SelectorInterruptException("Selector is interrupted or no channels are ready");
    }

    private void handleSelectedKey(NIOServerSession nioServerSession, NIOEvent nioEvent) throws IOException {
        if (nioEvent.isNewConnection()) {
            handleNewConnectionEvent(nioServerSession, nioEvent);
        } else if (nioEvent.isNewMessage()) {
            handleReadableEvent(nioServerSession, nioEvent);
        }
    }

    private void handleNewConnectionEvent(NIOServerSession nioServerSession, NIOEvent nioEvent) throws IOException {
        nioServerSession.acceptConnection(nioEvent);
        MetricUtils.incrementConcurrentUsers();
    }

    private void handleReadableEvent(NIOServerSession nioServerSession, NIOEvent nioEvent) throws IOException {
        nioServerSession.handleReadEvent(nioEvent);
    }

    private NetworkSelector openNetworkSelector() throws IOException {
        return new NetworkSelector(Selector.open());
    }
}
