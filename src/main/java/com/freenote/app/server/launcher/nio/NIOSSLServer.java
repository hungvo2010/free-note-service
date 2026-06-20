package com.freenote.app.server.launcher.nio;

import com.freenote.app.server.core.nio.NIOWebSocketServer;
import com.freenote.app.server.core.config.SSLConfig;
import com.freenote.app.server.core.config.ServerSocketConfig;
import com.freenote.app.server.core.nio.NIOIncomingSocketHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NIOSSLServer {
    private static final Logger log = LogManager.getLogger(NIOSSLServer.class);

    // Main entrypoint
    public static void main(String[] args) throws Exception {
        NIOWebSocketServer server = NIOWebSocketServer.builder()
                .socketConfig(new ServerSocketConfig(8443))
                .sslConfig(SSLConfig.builder().build())
                .handler(new NIOIncomingSocketHandler())
                .build();
        server.start();
    }
}
