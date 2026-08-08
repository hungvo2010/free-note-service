package com.freedraw.legacy;

import com.freenote.app.server.core.config.AppConfig;
import com.freenote.app.server.core.nio.NIOWebSocketServer;
import com.freenote.app.server.core.config.SSLConfig;
import com.freenote.app.server.core.config.ServerSocketConfig;
import com.freenote.app.server.core.nio.NIOIncomingSocketHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SSLFreeNoteServer {
    private static final Logger log = LogManager.getLogger(SSLFreeNoteServer.class);

    public static void main(String[] args) throws Exception {
        int port = AppConfig.getInt("server.ssl.port", 8189);
        String keystorePath = AppConfig.get("server.ssl.keystore.path", "keystore.p12");
        String keystorePassword = AppConfig.get("server.ssl.keystore.password", "changeit");

        log.info("Starting SSL server with keystore: {}", keystorePath);

        NIOWebSocketServer server = NIOWebSocketServer.builder()
                .sslConfig(new SSLConfig(keystorePath, keystorePassword))
                .socketConfig(new ServerSocketConfig(port))
                .handler(new NIOIncomingSocketHandler())
                .build();
        server.start();
    }
}
