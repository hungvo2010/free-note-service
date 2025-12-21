package com.freedraw;

import com.freenote.app.server.connections.IncomingSocketHandlerImpl;
import com.freenote.app.server.core.ServerBootStart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FreeNoteServer {
    private static final Logger log = LogManager.getLogger(FreeNoteServer.class);

    public static void main(String[] args) {
        int port = args.length >= 1 ? Integer.parseInt(args[0]) : 8189;
        run(port);
    }

    public static void run(int port) {
        var serverHolder = new ServerBootStart(port);
        try {
            log.info("Server started on port {}", port);
            serverHolder.start(new IncomingSocketHandlerImpl());
        } catch (Exception e) {
            log.error("Error starting server", e);
        }
        log.info("Server stopped");
    }
}