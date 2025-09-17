package com.freenote.app.server;

import com.freenote.app.server.factory.ClientFrameFactory;
import com.freenote.app.server.util.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class SSLWebSocketClient {

    private static final Logger log = LogManager.getLogger(SSLWebSocketClient.class);
    private SSLSocket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean connected = false;

    public static void main(String[] args) {
        try {
            SSLWebSocketClient client = new SSLWebSocketClient();
            client.connect("localhost", 8443, "/example");

            Thread.sleep(1500); // Wait for connection to stabilize

            // Send a test message
            client.sendMessage("Hello from raw SSL WebSocket client!");

            // Listen for messages for 10 seconds
            long endTime = System.currentTimeMillis() + 50_000;
            while (System.currentTimeMillis() < endTime && client.isConnected()) {
                String message = client.receiveMessage();
                if (message != null) {
                    log.info("Received: {}", message);
                }
                Thread.sleep(100);
            }

//            client.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connect(String host, int port, String path) throws Exception {
        // Create SSL context with custom certificates
        SSLContext sslContext = createSSLContext();

        // Create SSL socket factory
        SSLSocketFactory socketFactory = sslContext.getSocketFactory();

        // Create SSL socket
        socket = (SSLSocket) socketFactory.createSocket(host, port);
        socket.startHandshake();

        // Create streams
        OutputStream outputStream = socket.getOutputStream();
        InputStream inputStream = socket.getInputStream();
        writer = new PrintWriter(outputStream, true);
        reader = new BufferedReader(new InputStreamReader(inputStream));

        // Perform WebSocket handshake
        performWebSocketHandshake(host, path);

        connected = true;
        log.info("Connected to WebSocket server");
    }

    private void performWebSocketHandshake(String host, String path) throws Exception {
        // Generate WebSocket key
        byte[] keyBytes = new byte[16];
        new java.security.SecureRandom().nextBytes(keyBytes);
        String wsKey = Base64.getEncoder().encodeToString(keyBytes);

        // Send HTTP upgrade request
        writer.print("GET " + path + " HTTP/1.1\r\n");
        writer.print("Host: " + host + "\r\n");
        writer.print("Upgrade: websocket\r\n");
        writer.print("Connection: Upgrade\r\n");
        writer.print("Sec-WebSocket-Key: " + wsKey + "\r\n");
        writer.print("Sec-WebSocket-Version: 13\r\n");
        writer.print("\r\n");
        writer.flush();

        // Read response
        String statusLine = reader.readLine();
        if (!statusLine.contains("101")) {
            throw new IOException("WebSocket handshake failed: " + statusLine);
        }

        // Read headers and verify handshake
        String line;
        boolean upgradeFound = false;
        boolean connectionFound = false;
        String acceptKey = null;

        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            String[] parts = line.split(": ", 2);
            if (parts.length == 2) {
                String headerName = parts[0].toLowerCase();
                String headerValue = parts[1];

                if ("upgrade".equals(headerName) && "websocket".equalsIgnoreCase(headerValue)) {
                    upgradeFound = true;
                } else if ("connection".equals(headerName) && headerValue.toLowerCase().contains("upgrade")) {
                    connectionFound = true;
                } else if ("sec-websocket-accept".equals(headerName)) {
                    acceptKey = headerValue;
                }
            }
        }

        // Verify handshake response
        if (!upgradeFound || !connectionFound || acceptKey == null) {
            throw new IOException("Invalid WebSocket handshake response");
        }

        // Verify accept key
        String expectedAccept = generateAcceptKey(wsKey);
        if (!expectedAccept.equals(acceptKey)) {
            throw new IOException("Invalid Sec-WebSocket-Accept key");
        }

        log.info("WebSocket handshake successful");
    }

    private String generateAcceptKey(String wsKey) throws Exception {
        String magic = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        String combined = wsKey + magic;
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] hash = sha1.digest(combined.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    public void sendMessage(String message) throws IOException {
        if (!connected) {
            throw new IOException("Not connected to WebSocket server");
        }

        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        sendWebSocketFrame(0x81, payload); // Text frame with FIN bit set
        log.info("Send: {}", message);
    }

    private void sendWebSocketFrame(int opcode, byte[] payload) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        outputStream.flush();

        IOUtils.writeOutPut(outputStream, new ClientFrameFactory().createTextFrame("Test"));

//        // First byte: FIN + opcode
//        outputStream.write(opcode);
//
//        // Payload length and masking
//        // Client frames must be masked
//        int length = payload.length;
//
//        if (length < 126) {
//            outputStream.write(0x80 | length);
//        } else if (length < 65536) {
//            outputStream.write(0x80 | 126);
//            outputStream.write((length >> 8) & 0xFF);
//            outputStream.write(length & 0xFF);
//        } else {
//            outputStream.write(0x80 | 127);
//            // Write 64-bit length (simplified for int range)
//            for (int i = 7; i >= 0; i--) {
//                outputStream.write((int) ((long) length >> (8 * i)) & 0xFF);
//            }
//        }
//
//        // Masking key (if masked)
//        byte[] maskingKey = null;
//        maskingKey = new byte[4];
//        new SecureRandom().nextBytes(maskingKey);
//        outputStream.write(maskingKey);
//
//        // Payload (masked if required)
//        for (int i = 0; i < payload.length; i++) {
//            outputStream.write(payload[i] ^ maskingKey[i % 4]);
//        }
//
//        outputStream.flush();
    }

    public String receiveMessage() throws IOException {
        if (!connected) {
            return null;
        }

        try {
            InputStream inputStream = socket.getInputStream();

            // Check if data is available
            if (inputStream.available() == 0) {
                return null;
            }

            // Read WebSocket frame header
            int firstByte = inputStream.read();
            if (firstByte == -1) {
                connected = false;
                return null;
            }

            boolean fin = (firstByte & 0x80) != 0;
            int opcode = firstByte & 0x0F;

            // Handle different opcodes
            if (opcode == 0x8) { // Close frame
                connected = false;
                return null;
            } else if (opcode == 0x9) { // Ping frame
                // Send pong response
                sendWebSocketFrame(0x8A, new byte[0]);
                return null;
            } else if (opcode != 0x1 && opcode != 0x2) { // Not text or binary
                return null;
            }

            int secondByte = inputStream.read();
            if (secondByte == -1) {
                connected = false;
                return null;
            }

            boolean masked = (secondByte & 0x80) != 0;
            int length = secondByte & 0x7F;

            // Read extended payload length
            if (length == 126) {
                length = (inputStream.read() << 8) | inputStream.read();
            } else if (length == 127) {
                // Skip first 4 bytes (assuming length fits in int)
                inputStream.read(); inputStream.read(); inputStream.read(); inputStream.read();
                length = (inputStream.read() << 24) | (inputStream.read() << 16) |
                        (inputStream.read() << 8) | inputStream.read();
            }

            // Read masking key if present
            byte[] maskingKey = null;
            if (masked) {
                maskingKey = new byte[4];
                inputStream.read(maskingKey);
            }

            // Read payload
            byte[] payload = new byte[length];
            int totalRead = 0;
            while (totalRead < length) {
                int read = inputStream.read(payload, totalRead, length - totalRead);
                if (read == -1) {
                    connected = false;
                    return null;
                }
                totalRead += read;
            }

            // Unmask payload if masked
            if (masked && maskingKey != null) {
                for (int i = 0; i < payload.length; i++) {
                    payload[i] ^= maskingKey[i % 4];
                }
            }

            return new String(payload, StandardCharsets.UTF_8);

        } catch (IOException e) {
            connected = false;
            throw e;
        }
    }

    public boolean isConnected() {
        return connected && socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void disconnect() throws IOException {
        if (connected) {
            // Send close frame
            sendWebSocketFrame(0x88, new byte[0]);
            connected = false;
        }

        if (reader != null) {
            reader.close();
        }
        if (writer != null) {
            writer.close();
        }
        if (socket != null) {
            socket.close();
        }

        log.info("Disconnected from WebSocket server");
    }

    private SSLContext createSSLContext() throws Exception {
        log.info("Loading SSL certificates...");

        // Create a custom trust store
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);

        // Load the server certificate from PEM file
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            FileInputStream certFile = new FileInputStream("server-cert.pem");
            X509Certificate serverCert = (X509Certificate) cf.generateCertificate(certFile);
            certFile.close();

            trustStore.setCertificateEntry("server", serverCert);
            log.info("Loaded server certificate from server-cert.pem");
        } catch (Exception e) {
            System.err.println("Warning: Could not load server-cert.pem - " + e.getMessage());
        }

        // Load the PKCS12 truststore if available
        try {
            KeyStore p12TrustStore = KeyStore.getInstance("PKCS12");
            FileInputStream p12File = new FileInputStream("truststore.p12");
            p12TrustStore.load(p12File, "changeit".toCharArray()); // Change password as needed
            p12File.close();

            // Add certificates from p12 to main truststore
            java.util.Enumeration<String> aliases = p12TrustStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (p12TrustStore.isCertificateEntry(alias)) {
                    trustStore.setCertificateEntry("p12_" + alias, p12TrustStore.getCertificate(alias));
                }
            }
            log.info("Loaded certificates from truststore.p12");
        } catch (Exception e) {
            System.err.println("Warning: Could not load truststore.p12 - " + e.getMessage());
        }

        // Create a custom TrustManager that uses our trust store
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // Create SSL context
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        log.info("SSL context created successfully");
        return sslContext;
    }
}
