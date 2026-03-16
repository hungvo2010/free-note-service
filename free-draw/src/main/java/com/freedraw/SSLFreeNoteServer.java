package com.freedraw;

import com.freenote.app.server.core.DefaultIncomingConnectionHandler;
import com.freenote.app.server.core.WebSocketServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SSLFreeNoteServer {
    private static final Logger log = LogManager.getLogger(SSLFreeNoteServer.class);

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("SSL_PORT", "8189"));
        String keystorePath = System.getenv().getOrDefault("KEYSTORE_PATH", "keystore.p12");
        String keystorePassword = System.getenv().getOrDefault("KEYSTORE_PASSWORD", "changeit");

        log.info("Starting SSL server with keystore: {}", keystorePath);

        WebSocketServer server = WebSocketServer.builder()
                .port(port)
                .useSSL(true)
                .keystorePath(keystorePath)
                .keystorePassword(keystorePassword)
                .handler(new DefaultIncomingConnectionHandler())
                .build();
        server.start();
    }
}
