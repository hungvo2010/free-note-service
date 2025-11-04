package com.freenote.app.server.example;

import com.freenote.app.server.core.ServerHolder;
import com.freenote.app.server.handler.impl.WebSocketHandlerImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class SimpleServer {
    private static final Logger log = LogManager.getLogger(SimpleServer.class);

    public static void main(String[] args) {
        int port = args.length >= 1 ? Integer.parseInt(args[0]) : 8189;
        run(port);
    }

    public static List<Future<Void>> run(int port) {
        List<Future<Void>> futures = new ArrayList<>();
        var serverHolder = new ServerHolder(port);
        try {
            log.info("Server started on port {}", port);
            serverHolder.start(new WebSocketHandlerImpl());
        } catch (Exception e) {
            log.error("Error starting server", e);
        }
        log.info("Server stopped");
        return futures;
    }
}
