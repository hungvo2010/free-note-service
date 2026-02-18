package com.freenote.app.server.socket;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.FileInputStream;
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
        ks.load(new FileInputStream(keystorePath), passwordChars);

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
