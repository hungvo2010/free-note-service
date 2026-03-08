package com.freenote.app.server.demo;

import com.freenote.app.server.core.IncomingSocketHandler;
import com.freenote.app.server.core.WebSocketServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SimpleServer {
    private static final Logger log = LogManager.getLogger(SimpleServer.class);

    public static void main(String[] args) throws Exception {
        int port = args.length >= 1 ? Integer.parseInt(args[0]) : 8189;
        run(port);
    }

    public static void run(int port) throws Exception {
        WebSocketServer server = WebSocketServer.builder()
                .port(port)
                .useSSL(false)
                .handler(new IncomingSocketHandler())
                .build();
        server.start();
    }
}
