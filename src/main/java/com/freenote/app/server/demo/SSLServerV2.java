package com.freenote.app.server.demo;


import com.freenote.app.server.core.ServerBootstrap;
import com.freenote.app.server.core.v2.IncomingConnectionHandlerV2;
import com.freenote.app.server.core.v2.NIOIncomingConnectionHandlerImpl;
import com.freenote.app.server.socket.SSLSocket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SSLServerV2 {
    private static final Logger log = LogManager.getLogger(SSLServerV2.class);
    private final int port;
    private final String keystorePath;
    private final String password;
    private final IncomingConnectionHandlerV2 handler;

    public SSLServerV2(int port, String keystorePath, String password, IncomingConnectionHandlerV2 handler) {
        this.port = port;
        this.keystorePath = keystorePath;
        this.password = password;
        this.handler = handler;
    }

    public void start() throws Exception {
        log.info("Starting SSL Server on port {}", this.port);
        var serverBootstrap = new ServerBootstrap(new SSLSocket(this.keystorePath, this.password));
        serverBootstrap.setPort(this.port);
        serverBootstrap.start(this.handler);
    }

    // Main entrypoint
    public static void main(String[] args) throws Exception {
        SSLServerV2 server = new SSLServerV2(
                8443,
                "keystore.p12",
                "changeit",
                new NIOIncomingConnectionHandlerImpl() // can swap this with WebSocketHandler later
        );
        server.start();
    }
}

