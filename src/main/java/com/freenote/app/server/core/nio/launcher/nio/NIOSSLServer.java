package com.freenote.app.server.core.nio.launcher.nio;

import com.freenote.app.server.core.config.SSLConfig;
import com.freenote.app.server.core.config.ServerSocketConfig;
import com.freenote.app.server.core.legacy.WebSocketServer;
import com.freenote.app.server.core.nio.NIOIncomingSocketHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NIOSSLServer {
    private static final Logger log = LogManager.getLogger(NIOSSLServer.class);

    // Main entrypoint
    public static void main(String[] args) throws Exception {
        WebSocketServer server = WebSocketServer.builder()
                .socketConfig(new ServerSocketConfig(8443))
                .sslConfig(SSLConfig.builder().build())
                .handler(new NIOIncomingSocketHandler())
                .build();
        server.start();
    }
}
