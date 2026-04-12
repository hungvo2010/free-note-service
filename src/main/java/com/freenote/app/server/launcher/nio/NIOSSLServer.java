package com.freenote.app.server.launcher.nio;

import com.freenote.app.server.core.nio.NIOModernIncomingSocketHandler;
import com.freenote.app.server.core.nio.NIOWebSocketServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NIOSSLServer {
    private static final Logger log = LogManager.getLogger(NIOSSLServer.class);

    // Main entrypoint
    public static void main(String[] args) throws Exception {
        NIOWebSocketServer server = NIOWebSocketServer.builder()
                .port(8443)
                .useSSL(true)
                .keystorePath("keystore.p12")
                .keystorePassword("changeit")
                .handler(new NIOModernIncomingSocketHandler())
                .build();
        server.start();
    }
}
