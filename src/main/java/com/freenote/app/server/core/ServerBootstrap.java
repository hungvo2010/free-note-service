package com.freenote.app.server.core;

import com.freenote.app.server.core.v2.HandShakeState;
import com.freenote.app.server.core.v2.IncomingConnectionHandlerV2;
import com.freenote.app.server.socket.RawSocket;
import com.freenote.app.server.socket.ServerSocketFactory;
import lombok.AllArgsConstructor;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@AllArgsConstructor
public class ServerBootstrap {
    private static final int availableProcessors = Runtime.getRuntime().availableProcessors();
    private ExecutorService executorService = Executors.newFixedThreadPool(availableProcessors);
    private static final Logger log = LogManager.getLogger(ServerBootstrap.class);
    private ServerSocketFactory serverSocketFactory = new RawSocket();
    @Setter
    private int port = 8189;

    public ServerBootstrap(ServerSocketFactory serverSocketFactory) {
        this.serverSocketFactory = serverSocketFactory;
    }

    public ServerBootstrap() {
    }

    public void start(IncomingConnectionHandler handler) throws Exception {
        printNumberProcessors();
        try (var serverSocket = serverSocketFactory.createServerSocket(this.port)) {
            while (!serverSocket.isClosed()) {
                log.info("Waiting for connection on port {}", this.port);
                var socket = serverSocket.accept(); // block method
                log.info("Accepted connection from {}", socket.getRemoteSocketAddress());
                this.executorService.submit(() -> {
                    try {
                        handler.handle(socket);
                    } catch (Exception e) {
                        log.error("Error handling connection", e);
                    }
                });
            }
        }
    }

    private static void printNumberProcessors() {
        log.info("Number of available processors: {}", availableProcessors);
    }

    public void start(IncomingConnectionHandlerV2 handler) throws IOException {
        var selector = Selector.open();
        try (var serverSocketChannel = ServerSocketChannel.open()) {
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(this.port));
            log.info("Starting server on port {}", this.port);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            Future future = this.executorService.submit(() -> {
                try {
                    startThreadSelector(selector, handler);
                } catch (IOException e) {
                    log.error("Error in server thread", e);
                }
            });
            try {
                printNumberProcessors();
                future.get();
            } catch (Exception e) {
                log.error("Error in server thread", e);
            }
        }
    }

    private void startThreadSelector(Selector selector, IncomingConnectionHandlerV2 handler) throws IOException {
        while (true) {
            int numReadyChannels = selector.select();
            if (numReadyChannels == 0) continue;

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                if (key.isAcceptable()) {
                    ServerSocketChannel server = (ServerSocketChannel) key.channel();
                    SocketChannel client = server.accept();
                    if (client != null) {
                        client.configureBlocking(false);
                        HandShakeState state = new HandShakeState();
                        client.register(selector, SelectionKey.OP_READ, state);
                        System.out.println("Accepted connection from " + client.getRemoteAddress());
                    }
                } else if (key.isReadable()) {
                    var channel = (SocketChannel) key.channel();
                    HandShakeState state = (HandShakeState) key.attachment();
                    if (state.isFirstRead()) {
                        state.setFirstRead(false);
                        log.info("First read from {}, performing handshake", channel.getRemoteAddress());
                        var upgradeRequest = handler.handShake(channel, state.getByteBuffer());
                        state.setUpgradeRequest(upgradeRequest);
                    } else {
                        log.info("Subsequent read from {}", channel.getRemoteAddress());
                        handler.handleInComingMessage(channel, state.getByteBuffer(), state.getUpgradeRequest());
                    }
                }
                keyIterator.remove();
            }
        }
    }
}
