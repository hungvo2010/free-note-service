package com.freenote.app.server.demo;

import com.freenote.app.server.core.v2.NIOIncomingConnectionHandlerImpl;
import com.freenote.app.server.core.v2.WebSocketServerV2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SSLServerV2 {
    private static final Logger log = LogManager.getLogger(SSLServerV2.class);

    // Main entrypoint
    public static void main(String[] args) throws Exception {
        WebSocketServerV2 server = WebSocketServerV2.builder()
                .port(8443)
                .useSSL(true)
                .keystorePath("keystore.p12")
                .keystorePassword("changeit")
                .handler(new NIOIncomingConnectionHandlerImpl())
                .build();
        server.start();
    }
}
