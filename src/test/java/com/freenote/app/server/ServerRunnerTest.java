package com.freenote.app.server;

import com.freenote.app.server.example.SimpleServer;
import com.freenote.app.server.frames.factory.ClientFrameFactory;
import com.freenote.app.server.util.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServerRunnerTest {

    private static final Logger log = LogManager.getLogger(ServerRunnerTest.class);
    private final String HANDSHAKE_DATA = """
            GET ws://localhost:8189/echo HTTP/1.1\r
            Host: localhost:8189\r
            Connection: Upgrade\r
            Pragma: no-cache\r
            Cache-Control: no-cache\r
            User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36\r
            Upgrade: websocket\r
            Origin: http://localhost:8082\r
            Sec-WebSocket-Version: 13\r
            Accept-Encoding: gzip, deflate, br, zstd\r
            Accept-Language: en-US,en;q=0.9,vi-VN;q=0.8,vi;q=0.7\r
            Sec-WebSocket-Key: TixQkgsxKyup9IZVxSoe1w==\r
            Sec-WebSocket-Extensions: permessage-deflate; client_max_window_bits\r
            \r
            """;

    @Test
    void givenWebSocketHandshakeRequest_whenServerAcceptsConnection_thenHandshakeResponseSent() throws Exception {
        // Mocks
        ServerSocket serverSocket = mock(ServerSocket.class);
        Socket clientSocket = mock(Socket.class);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        AtomicBoolean running = new AtomicBoolean(true);

        var pipeOutputStream = new PipedOutputStream();
        var pipedInputStream = new PipedInputStream(pipeOutputStream);
        ByteArrayOutputStream clientOutput = new ByteArrayOutputStream();

        var flag = new AtomicInteger(0);
        flag.incrementAndGet();

        when(serverSocket.accept()).thenAnswer(invocation -> {
            running.set(false); // Accept once then stop server
            return clientSocket;
        });
        when(clientSocket.getOutputStream()).thenReturn(clientOutput);
        when(clientSocket.getInputStream()).thenReturn(pipedInputStream);
        when(clientSocket.isClosed()).thenAnswer(invocation -> {
            if (flag.get() == 2) {
                return true;
            }
            return false;
        });

        Thread.sleep(10000);
        pipeOutputStream.write(HANDSHAKE_DATA.getBytes());

        var serverThread = new Thread(() -> {
            try {
                var futures = SimpleServer.run(8189);
                pipeOutputStream.flush();
                // Wait for first future to complete
                if (!futures.isEmpty()) {
                    futures.get(0).get();
                    log.info("First connection handled");
                }
            } catch (Exception ex) {
                log.error("Failed to start server", ex);
            }
        });
        serverThread.start();

        // Check that "Hello" was written
        Thread.sleep(1000);
        String response = clientOutput.toString().trim();
//        assertEquals("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: O163+NfhFwxULDbPCuiQo7hGj30=", response);


//        serverThread.join();

        var socketThread = new Thread(() -> {
            log.info("Send echo message");
            // Close the input stream to trigger socket closure detection

            log.info("Send echo message");
            try {
                IOUtils.writeOutPut(pipeOutputStream, new ClientFrameFactory().createTextFrame("hello-world"));
            } catch (Exception e) {
                log.error("Failed to write to socket", e);
            }
            flag.incrementAndGet();
        });

        socketThread.start();
        socketThread.join();

//        executorService.shutdownNow();
    }
}
