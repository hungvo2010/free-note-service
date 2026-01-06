package com.freenote.app.server.core;

import com.freenote.app.server.socket.RawSocket;
import com.freenote.app.server.socket.ServerSocketFactory;
import lombok.AllArgsConstructor;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@AllArgsConstructor
public class ServerBootstrap {
    private ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static final Logger log = LogManager.getLogger(ServerBootstrap.class);
    private ServerSocketFactory serverSocketFactory = new RawSocket();
    @Setter
    private int port = 8189;

    public ServerBootstrap(int port) {
        this.port = port;
    }

    public ServerBootstrap(ServerSocketFactory serverSocketFactory) {
        this.serverSocketFactory = serverSocketFactory;
    }

    public ServerBootstrap(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void start(IncomingConnectionHandler handler) throws Exception {
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
}
