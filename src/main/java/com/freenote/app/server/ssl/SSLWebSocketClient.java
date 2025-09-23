package com.freenote.app.server.ssl;

import com.freenote.app.server.factory.ClientFrameFactory;
import com.freenote.app.server.frames.base.DataFrame;
import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.util.FrameUtil;
import com.freenote.app.server.util.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
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
            client.connect("localhost", 8443, "/echo");

            Thread.sleep(1500); // Wait for connection to stabilize

            for (int i = 0; i < 10; ++i) {
//                Thread.sleep(new Random().nextLong(1500, 10000));
                client.sendMessage("Hello from raw SSL WebSocket client!");
            }

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
            log.error("Error connecting to WebSocket server", e);
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

        IOUtils.writeOutPut(outputStream, new ClientFrameFactory().createTextFrame(new String(payload, StandardCharsets.UTF_8)));
    }

    public String receiveMessage() throws IOException {
        if (!connected) {
            return null;
        }

        try {
            InputStream inputStream = socket.getInputStream();

            // Set a read timeout to avoid infinite blocking
            socket.setSoTimeout(100); // 100ms timeout

            // Use the same reliable reading approach as EchoHandler
            byte[] actualFrameBytes;
            try {
                actualFrameBytes = getAllBytes(inputStream);
            } catch (SocketTimeoutException e) {
                return null; // No data available, return null
            }

            if (actualFrameBytes == null) {
                connected = false;
                return null;
            }

            log.info("Received raw bytes length: {}", actualFrameBytes.length);
            log.info("First 10 bytes: {}", Arrays.toString(Arrays.copyOf(actualFrameBytes, Math.min(10, actualFrameBytes.length))));

            // Use DataFrame.fromRawFrameBytes to parse
            WebSocketFrame frame = DataFrame.fromRawFrameBytes(actualFrameBytes);

            log.info("Parsed frame - FIN: {}, Opcode: {}, Masked: {}, Payload Length: {}",
                    frame.isFin(), frame.getOpcode(), frame.isMasked(), frame.getPayloadLength());

            // Extract payload and convert to string
            byte[] payload = frame.getPayloadData();
            if (frame.isMasked()) {
                payload = FrameUtil.maskPayload(payload, frame.getMaskingKey());
            }

            String message = new String(payload, StandardCharsets.UTF_8);
            log.info("Decoded message: {}", message);
            return message;

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

    private byte[] getAllBytes(InputStream inputStream) throws IOException {
        // Read first byte (opcode)
        int firstByte = inputStream.read();
        if (firstByte == -1) {
            return null;
        }

        // Read second byte (payload length + mask)
        int secondByte = inputStream.read();
        if (secondByte == -1) {
            return null;
        }

        // Calculate total frame length needed
        int baseLength = 2; // opcode + length/mask byte
        int payloadLength = secondByte & 0x7F;
        boolean masked = (secondByte & 0x80) != 0;

        // Handle extended payload length
        if (payloadLength == 126) {
            baseLength += 2; // 2 more bytes for length
        } else if (payloadLength == 127) {
            baseLength += 8; // 8 more bytes for length
        }

        // Add masking key length
        if (masked) {
            baseLength += 4;
        }

        // Add actual payload length (simplified for small frames)
        int totalFrameLength = baseLength + (payloadLength < 126 ? payloadLength : 0);

        // Read complete frame
        byte[] frameData = new byte[totalFrameLength];
        frameData[0] = (byte) firstByte;
        frameData[1] = (byte) secondByte;

        int totalRead = 2;
        while (totalRead < totalFrameLength) {
            int read = inputStream.read(frameData, totalRead, totalFrameLength - totalRead);
            if (read == -1) {
                return null;
            }
            totalRead += read;
        }

        return frameData;
    }
}
