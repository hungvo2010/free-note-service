package com.freenote.app.server.example;


import com.freenote.app.server.core.ServerBootStart;
import com.freenote.app.server.handler.impl.IncomingSocketHandlerImpl;
import com.freenote.app.server.factory.SSLSocketImpl;
import com.freenote.app.server.handler.IncomingConnectionHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SSLServer {
    private static final Logger log = LogManager.getLogger(SSLServer.class);
    private final int port;
    private final String keystorePath;
    private final String password;
    private final IncomingConnectionHandler handler;

    public SSLServer(int port, String keystorePath, String password, IncomingConnectionHandler handler) {
        this.port = port;
        this.keystorePath = keystorePath;
        this.password = password;
        this.handler = handler;
    }

    public void start() throws Exception {
        log.info("Starting SSL Server on port {}", this.port);
        var serverHolder = new ServerBootStart(new SSLSocketImpl(this.keystorePath, this.password));
        serverHolder.setPort(this.port);
        serverHolder.start(this.handler);
    }

    // Main entrypoint
    public static void main(String[] args) throws Exception {
        SSLServer server = new SSLServer(
                8443,
                "keystore.p12",
                "changeit",
                new IncomingSocketHandlerImpl() // can swap this with WebSocketHandler later
        );
        server.start();
    }
}

