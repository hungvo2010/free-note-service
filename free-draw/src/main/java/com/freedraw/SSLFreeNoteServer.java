package com.freedraw;

import com.freenote.app.server.core.IncomingConnectionHandler;
import com.freenote.app.server.core.IncomingSocketHandlerImpl;
import com.freenote.app.server.core.ServerBootstrap;
import com.freenote.app.server.socket.SSLSocket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SSLFreeNoteServer {
    private static final Logger log = LogManager.getLogger(SSLFreeNoteServer.class);
    private final int port;
    private final String keystorePath;
    private final String password;
    private final IncomingConnectionHandler handler;

    public SSLFreeNoteServer(int port, String keystorePath, String password, IncomingConnectionHandler handler) {
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
         int port = Integer.parseInt(System.getenv().getOrDefault("SSL_PORT", "8189"));
        String keystorePath = System.getenv().getOrDefault("KEYSTORE_PATH", "keystore.p12");
        String keystorePassword = System.getenv().getOrDefault("KEYSTORE_PASSWORD", "changeit");
        
        log.info("Starting SSL server with keystore: {}", keystorePath);
        
        SSLFreeNoteServer server = new SSLFreeNoteServer(
                port,
                keystorePath,
                keystorePassword,
                new IncomingSocketHandlerImpl()
        );
        server.start();
    }
}

