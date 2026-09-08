package com.freenote.app.server.core.legacy.launcher;

import com.freenote.app.server.core.config.SSLConfig;
import com.freenote.app.server.core.config.ServerSocketConfig;
import com.freenote.app.server.core.legacy.ThreadPerConnectionHandler;
import com.freenote.app.server.core.legacy.WebSocketServer;
import com.freenote.app.server.core.legacy.startup.LegacyBootstrap;

public class SSLServer {
    public static void main(String[] args) throws Exception {
        WebSocketServer server = WebSocketServer.builder()
                .socketConfig(new ServerSocketConfig(8443))
                .sslConfig(SSLConfig.builder().build())
                .handler(new ThreadPerConnectionHandler())
                .serverBootstrap(new LegacyBootstrap())
                .build();
        server.start();
    }
}
