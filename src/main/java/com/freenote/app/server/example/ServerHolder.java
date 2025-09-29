package com.freenote.app.server.example;

import com.freenote.app.server.factory.RawSocketFactory;
import com.freenote.app.server.factory.ServerSocketFactory;
import com.freenote.app.server.handler.ConnectionHandler;
import lombok.AllArgsConstructor;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@AllArgsConstructor
public class ServerHolder {
    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static final Logger log = LogManager.getLogger(ServerHolder.class);
    private final ServerSocketFactory serverSocketFactory;
    @Setter
    private int port = 8189;

    public ServerHolder() {
        this.serverSocketFactory = new RawSocketFactory();
    }

    public ServerHolder(int port) {
        this.serverSocketFactory = new RawSocketFactory();
        this.port = port;
    }

    public ServerHolder(ServerSocketFactory serverSocketFactory) {
        this.serverSocketFactory = serverSocketFactory;
    }

    public void start(ConnectionHandler handler) throws Exception {
        var serverSocket = serverSocketFactory.createServerSocket(this.port);
        while (!serverSocket.isClosed()) {
            var socket = serverSocket.accept();
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
