package com.freenote.app.server.core;

import com.freenote.app.server.factory.RawSocketImpl;
import com.freenote.app.server.factory.ServerSocketFactory;
import com.freenote.app.server.handler.ConnectionHandler;
import lombok.AllArgsConstructor;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@AllArgsConstructor
public class ServerBootStart {
    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static final Logger log = LogManager.getLogger(ServerBootStart.class);
    private ServerSocketFactory serverSocketFactory = new RawSocketImpl();
    @Setter
    private int port = 8189;

    public ServerBootStart() {
    }

    public ServerBootStart(int port) {
        this.port = port;
    }

    public ServerBootStart(ServerSocketFactory serverSocketFactory) {
        this.serverSocketFactory = serverSocketFactory;
    }

    public void start(ConnectionHandler handler) throws Exception {
        try (var serverSocket = serverSocketFactory.createServerSocket(this.port)) {
            while (!serverSocket.isClosed()) {
                log.info("Waiting for connection on port {}", this.port);
                var socket = serverSocket.accept(); // block method
                log.info("Accepted connection from {}", socket.getRemoteSocketAddress());
                Future res = this.executorService.submit(() -> {
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
