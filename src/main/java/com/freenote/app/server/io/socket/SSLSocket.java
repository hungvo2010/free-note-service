package com.freenote.app.server.io.socket;

import com.freenote.app.server.core.config.SSLConfig;
import com.freenote.app.server.core.config.ServerSocketConfig;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.security.KeyStore;

public class SSLSocket implements ServerSocketFactory {

    private final SSLConfig sslConfig;

    public SSLSocket(SSLConfig sslConfig) {
        this.sslConfig = sslConfig;
    }

    @Override
    public ServerSocket createServerSocket(ServerSocketConfig config) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        var passwordChars = sslConfig.getKeystorePassword().toCharArray();

        // Try loading from filesystem first, then from classpath
        InputStream is = null;
        try {
            File file = new File(this.sslConfig.getKeystorePath());
            if (file.exists()) {
                is = new FileInputStream(file);
            } else {
                is = getClass().getClassLoader().getResourceAsStream(this.sslConfig.getKeystorePath());
                if (is == null) {
                    throw new FileNotFoundException("Keystore not found: " + this.sslConfig.getKeystorePath());
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
        var serverSocket = (SSLServerSocket) factory.createServerSocket(config.port());

        // Don't require client authentication
        serverSocket.setNeedClientAuth(false);
        serverSocket.setWantClientAuth(false);

        return serverSocket;
    }
}
