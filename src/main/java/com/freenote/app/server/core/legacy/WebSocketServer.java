package com.freenote.app.server.core.legacy;

import com.freenote.app.server.core.config.SSLConfig;
import com.freenote.app.server.core.config.ServerSocketConfig;
import com.freenote.app.server.core.connection.IncomingConnectionHandler;
import com.freenote.app.server.core.legacy.startup.LegacyBootstrap;
import com.freenote.app.server.core.startup.ServerBootstrap;
import lombok.Builder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Builder
public class WebSocketServer {
    private static final Logger log = LogManager.getLogger(WebSocketServer.class);
    private ServerSocketConfig socketConfig;
    private SSLConfig sslConfig;
    private IncomingConnectionHandler handler;

    public void start() throws Exception {
        log.info("Starting WebSocket Server on port {}", socketConfig.port());

        ServerBootstrap bootstrap = new LegacyBootstrap();
        bootstrap.start(handler, socketConfig);
    }
}
