package com.freenote.app.server.core.nio;

import com.freenote.app.server.core.config.SSLConfig;
import com.freenote.app.server.core.config.ServerSocketConfig;
import com.freenote.app.server.core.connection.IncomingConnectionHandler;
import com.freenote.app.server.core.nio.startup.AsyncNIOServerBootstrap;
import com.freenote.app.server.core.startup.ServerBootstrap;
import lombok.Builder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Builder
public class NIOWebSocketServer {
    private static final Logger log = LogManager.getLogger(NIOWebSocketServer.class);
    private ServerSocketConfig socketConfig;
    private SSLConfig sslConfig;
    private IncomingConnectionHandler handler;

    public void start() throws Exception {
        log.info("Starting NIO WebSocket Server on port {}", socketConfig.port());

//        ServerBootstrap bootstrap = new NIOServerBootstrap();
        ServerBootstrap bootstrap = new AsyncNIOServerBootstrap();
        bootstrap.start(handler, socketConfig);
    }
}
