package com.freenote.app.server.launcher;

import com.freenote.app.server.core.legacy.WebSocketServer;
import com.freenote.app.server.core.config.SSLConfig;
import com.freenote.app.server.core.config.ServerSocketConfig;
import com.freenote.app.server.core.legacy.DefaultLegacySessionBasedConnectionHandler;
import com.freenote.app.server.core.legacy.LegacyConnectionAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SSLServer {
    private static final Logger log = LogManager.getLogger(SSLServer.class);

    public static void main(String[] args) throws Exception {
        WebSocketServer server = WebSocketServer.builder()
                .socketConfig(new ServerSocketConfig(8443))
                .sslConfig(SSLConfig.builder().build())
                .handler(new LegacyConnectionAdapter(new DefaultLegacySessionBasedConnectionHandler()))
                .build();
        server.start();
    }
}
