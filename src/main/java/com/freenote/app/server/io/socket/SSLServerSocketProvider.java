package com.freenote.app.server.io.socket;

import com.freenote.app.server.core.config.SSLConfig;
import com.freenote.app.server.core.config.ServerSocketConfig;
import com.freenote.app.server.exceptions.SocketCreationException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.*;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class SSLServerSocketProvider implements ServerSocketProvider {

    private final SSLConfig sslConfig;

    public SSLServerSocketProvider(SSLConfig sslConfig) {
        this.sslConfig = sslConfig;
    }

    @Override
    public ServerSocket createServerSocket(ServerSocketConfig config) throws SocketCreationException {
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            var passwordChars = sslConfig.getKeystorePassword().toCharArray();

            // Try loading from filesystem first, then from classpath
            initKeyStore(ks, passwordChars);

            // Init KeyManager with server private key
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, passwordChars);

            // Init SSL context
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(kmf.getKeyManagers(), null, null);

            // Create SSL server socket
            SSLServerSocketFactory factory = ctx.getServerSocketFactory();
            var serverSocket = (javax.net.ssl.SSLServerSocket) factory.createServerSocket(config.port());

            // Don't require client authentication
            serverSocket.setNeedClientAuth(false);
            serverSocket.setWantClientAuth(false);

            return serverSocket;
        } catch (Exception e) {
            throw new SocketCreationException("Failed to create SSL server socket", e);
        }
    }

    private void initKeyStore(KeyStore ks, char[] passwordChars) throws IOException, NoSuchAlgorithmException, CertificateException {
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
    }
}
