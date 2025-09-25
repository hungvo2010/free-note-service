package com.freenote.app.server.example;

import com.freenote.app.server.exceptions.AcceptConnectionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class SimpleServer {
    private static final Logger log = LogManager.getLogger(SimpleServer.class);

    public static void main(String[] args) throws IOException {
        int port = args.length >= 1 ? Integer.parseInt(args[0]) : 8189;
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try (var s = new ServerSocket(port)) {
            run(s, executorService, new AtomicBoolean(true));
        }
    }

    public static List<Future<Void>> run(ServerSocket serverSocket, ExecutorService executorService, AtomicBoolean running) throws IOException {
        List<Future<Void>> futures = new ArrayList<>();
        log.info("Server started on port: {}", serverSocket.getLocalPort());
        while (running.get()) {
            var incomingSocket = serverSocket.accept();
            futures.add((Future<Void>) executorService.submit(() -> {
                try {
                    incomingSocket.setSoTimeout(5000); // to make timeout after 5 seconds of blocking read
                    new WebSocketHandler().handle(incomingSocket);
                } catch (Exception e) {
                    log.error("Failed to accept connection", e);
                    throw new AcceptConnectionException(e);
                }
            }));
        }
        log.info("Server stopped");
        return futures;
    }
}
