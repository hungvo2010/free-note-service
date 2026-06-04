package com.freenote.app.server.core;

import com.freenote.app.server.core.config.SSLConfig;
import com.freenote.app.server.core.config.ServerSocketConfig;
import com.freenote.app.server.core.connection.IncomingConnectionHandler;
import com.freenote.app.server.core.startup.LegacyBootstrap;
import com.freenote.app.server.core.startup.ServerBootstrap;
import com.freenote.app.server.io.socket.RawSocket;
import com.freenote.app.server.io.socket.SSLSocket;
import com.freenote.app.server.io.socket.ServerSocketFactory;
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

        ServerSocketFactory socketFactory = sslConfig != null
                ? new SSLSocket(sslConfig)
                : new RawSocket();

        ServerBootstrap bootstrap = new LegacyBootstrap();
        bootstrap.start(handler, socketConfig);
    }
}
