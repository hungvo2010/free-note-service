package com.freedraw.legacy;

import com.freenote.app.server.core.nio.NIOModernIncomingSocketHandler;
import com.freenote.app.server.core.nio.NIOWebSocketServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SSLFreeNoteServer {
    private static final Logger log = LogManager.getLogger(SSLFreeNoteServer.class);

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("SSL_PORT", "8189"));
        String keystorePath = System.getenv().getOrDefault("KEYSTORE_PATH", "keystore.p12");
        String keystorePassword = System.getenv().getOrDefault("KEYSTORE_PASSWORD", "changeit");

        log.info("Starting SSL server with keystore: {}", keystorePath);

        NIOWebSocketServer server = NIOWebSocketServer.builder()
                .port(port)
                .useSSL(false)
                .handler(new NIOModernIncomingSocketHandler())
                .build();
        server.start();
    }
}
