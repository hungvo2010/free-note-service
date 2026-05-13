package com.freenote.app.server.launcher;

import com.freenote.app.server.core.WebSocketServer;
import com.freenote.app.server.core.legacy.DefaultLegacyIncomingConnectionHandler;

public class SimpleServer {

    public static void main(String[] args) throws Exception {
        int port = args.length >= 1 ? Integer.parseInt(args[0]) : 8189;
        run(port);
    }

    public static void run(int port) throws Exception {
        WebSocketServer server = WebSocketServer.builder()
                .port(port)
                .useSSL(false)
                .handler(new DefaultLegacyIncomingConnectionHandler())
                .build();
        server.start();
    }
}
