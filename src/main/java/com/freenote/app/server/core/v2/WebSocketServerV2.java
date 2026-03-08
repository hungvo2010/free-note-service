package com.freenote.app.server.core.v2;

import com.freenote.app.server.core.ServerBootstrap;
import lombok.Builder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Builder
public class WebSocketServerV2 {
    private static final Logger log = LogManager.getLogger(WebSocketServerV2.class);

    @Builder.Default
    private int port = 8189;
    @Builder.Default
    private boolean useSSL = false;
    @Builder.Default
    private String keystorePath = "keystore.p12";
    @Builder.Default
    private String keystorePassword = "changeit";
    @Builder.Default
    private IncomingConnectionHandlerV2 handler = new NIOIncomingConnectionHandlerImpl();

    public void start() throws Exception {
        log.info("Starting NIO WebSocket Server on port {} (SSL: {})", port, useSSL);

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.setPort(port);
        bootstrap.start(handler);
    }
}
