package com.freenote.app.server.demo;

import com.freenote.app.server.core.DefaultLegacyIncomingConnectionHandler;
import com.freenote.app.server.core.WebSocketServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SSLServer {
    private static final Logger log = LogManager.getLogger(SSLServer.class);

    public static void main(String[] args) throws Exception {
        WebSocketServer server = WebSocketServer.builder()
                .port(8443)
                .useSSL(true)
                .keystorePath("keystore.p12")
                .keystorePassword("changeit")
                .handler(new DefaultLegacyIncomingConnectionHandler())
                .build();
        server.start();
    }
}
