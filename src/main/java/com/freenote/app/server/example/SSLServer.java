package com.freenote.app.server.example;


import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;

public class SSLServer {
    public static void main(String[] args) throws Exception {
        // Load server keystore
        char[] passphrase = "changeit".toCharArray();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(new FileInputStream("keystore.p12"), passphrase);

        // Init KeyManager with server private key
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        // Init SSL context
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), null, null);

        // Create SSL server socket
        SSLServerSocketFactory factory = ctx.getServerSocketFactory();
        SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(8443);

        System.out.println("SSL Server listening on port 8443...");

        while (true) {
            try (SSLSocket socket = (SSLSocket) serverSocket.accept()) {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

                String line = in.readLine();
                System.out.println("Received: " + line);
                out.println("Echo: " + line);
            }
        }
    }
}
