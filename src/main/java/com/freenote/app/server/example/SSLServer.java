package com.freenote.app.server.example;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SSLServer {

    private static final Logger log = LogManager.getLogger(SSLServer.class);
    private final int port;
    private final String keystorePath;
    private final char[] password;
    private final ConnectionHandler handler;

    public SSLServer(int port, String keystorePath, char[] password, ConnectionHandler handler) {
        this.port = port;
        this.keystorePath = keystorePath;
        this.password = password;
        this.handler = handler;
    }

    public void start() throws Exception {
        // Load server keystore
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(new FileInputStream(keystorePath), password);

        // Init KeyManager with server private key
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, password);

        // Init SSL context
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), null, null);

        // Create SSL server socket
        SSLServerSocketFactory factory = ctx.getServerSocketFactory();
        SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(port);

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        log.info("SSL Server listening on port {}...", port);

        while (true) {
            SSLSocket socket = (SSLSocket) serverSocket.accept();
            executorService.submit(() -> {
                        try {
                            handler.handle(socket);
                        } catch (Exception e) {
                            log.error("Error handling connection", e);
                        }
                    }
            );
        }
    }

    // Main entrypoint
    public static void main(String[] args) throws Exception {
        SSLServer server = new SSLServer(
                8443,
                "keystore.p12",
                "changeit".toCharArray(),
                new WebSocketHandler() // can swap this with WebSocketHandler later
        );
        server.start();
    }
}

