package com.freenote.app.server.example;


import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.security.KeyStore;

/**
 * Generic SSL/TLS server that delegates handling of each client connection
 * to a user-provided ConnectionHandler.
 */
public class SSLServer {

    public interface ConnectionHandler {
        void handle(Socket socket) throws Exception;
    }

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

        System.out.println("SSL Server listening on port " + port + "...");

        while (true) {
            SSLSocket socket = (SSLSocket) serverSocket.accept();
            new Thread(() -> {
                try (socket) {
                    handler.handle(socket);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    // Example: Echo line-based handler
    public static class EchoHandler implements ConnectionHandler {
        @Override
        public void handle(Socket socket) throws Exception {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Received: " + line);
                out.println("Echo: " + line);
            }
        }
    }

    // Main entrypoint
    public static void main(String[] args) throws Exception {
        SSLServer server = new SSLServer(
                8443,
                "keystore.p12",
                "changeit".toCharArray(),
                new EchoHandler() // can swap this with WebSocketHandler later
        );
        server.start();
    }
}

