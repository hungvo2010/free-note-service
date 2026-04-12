package com.freenote.app.server.core;

import com.freenote.app.server.core.legacy.DefaultLegacyIncomingConnectionHandler;
import com.freenote.app.server.core.legacy.LegacyIncomingConnectionHandler;
import com.freenote.app.server.core.startup.ServerBootstrap;
import com.freenote.app.server.socket.RawSocket;
import com.freenote.app.server.socket.SSLSocket;
import com.freenote.app.server.socket.ServerSocketFactory;
import lombok.Builder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Builder
public class WebSocketServer {
    private static final Logger log = LogManager.getLogger(WebSocketServer.class);

    @Builder.Default
    private int port = 8189;
    @Builder.Default
    private boolean useSSL = false;
    @Builder.Default
    private String keystorePath = "keystore.p12";
    @Builder.Default
    private String keystorePassword = "changeit";
    @Builder.Default
    private LegacyIncomingConnectionHandler handler = new DefaultLegacyIncomingConnectionHandler();

    public void start() throws Exception {
        log.info("Starting WebSocket Server on port {} (SSL: {})", port, useSSL);
        
        ServerSocketFactory socketFactory = useSSL 
                ? new SSLSocket(keystorePath, keystorePassword) 
                : new RawSocket();
        
        ServerBootstrap bootstrap = new ServerBootstrap(socketFactory);
        bootstrap.setPort(port);
        bootstrap.start(handler);
    }
}
