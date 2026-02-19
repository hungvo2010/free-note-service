package com.freenote.app.server.socket;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.security.KeyStore;

public class SSLSocket implements ServerSocketFactory {
    private final String keystorePath;
    private final String password;

    public SSLSocket(String keystorePath, String password) {
        this.keystorePath = keystorePath;
        this.password = password;
    }

    @Override
    public ServerSocket createServerSocket(int port) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        var passwordChars = password.toCharArray();
        
        // Try loading from filesystem first, then from classpath
        InputStream is = null;
        try {
            java.io.File file = new java.io.File(keystorePath);
            if (file.exists()) {
                is = new java.io.FileInputStream(file);
            } else {
                is = getClass().getClassLoader().getResourceAsStream(keystorePath);
                if (is == null) {
                    throw new FileNotFoundException("Keystore not found: " + keystorePath);
                }
            }
            ks.load(is, passwordChars);
        } finally {
            if (is != null) {
                is.close();
            }
        }

        // Init KeyManager with server private key
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passwordChars);

        // Init SSL context
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), null, null);

        // Create SSL server socket
        SSLServerSocketFactory factory = ctx.getServerSocketFactory();
        var serverSocket = (javax.net.ssl.SSLServerSocket) factory.createServerSocket(port);

        // Don't require client authentication
        serverSocket.setNeedClientAuth(false);
        serverSocket.setWantClientAuth(false);

        return serverSocket;
    }
}
