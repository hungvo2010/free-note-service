package com.freenote.app.server.launcher.nio;

import com.freenote.app.server.core.NIOWebSocketServer;
import com.freenote.app.server.core.config.ServerSocketConfig;
import com.freenote.app.server.core.nio.NIOModernIncomingSocketHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NIOSimpleServer {
    private static final Logger log = LogManager.getLogger(NIOSimpleServer.class);

    public static void main(String[] args) throws Exception {
        int port = args.length >= 1 ? Integer.parseInt(args[0]) : 8189;
        run(port);
    }

    public static void run(int port) throws Exception {
        NIOWebSocketServer server = NIOWebSocketServer.builder()
                .socketConfig(new ServerSocketConfig(port))
                .handler(new NIOModernIncomingSocketHandler())
                .build();
        server.start();
    }
}
