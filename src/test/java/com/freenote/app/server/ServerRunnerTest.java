package com.freenote.app.server;

import com.freenote.app.server.example.SimpleServer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ServerRunnerTest {

    @Test
    void shouldAcceptSocketAndServeClient() throws Exception {
        // Mocks
        ServerSocket mockServerSocket = mock(ServerSocket.class);
        Socket mockSocket = mock(Socket.class);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        AtomicBoolean running = new AtomicBoolean(true);
        SimpleServer serverRunner = new SimpleServer();

        // Simulate accepting one connection then stop
        when(mockServerSocket.accept()).thenAnswer(invocation -> {
            running.set(false); // Accept once then stop server
            return mockSocket;
        });

        // Capture output written to client socket
        ByteArrayOutputStream clientOutput = new ByteArrayOutputStream();
        when(mockSocket.getOutputStream()).thenReturn(clientOutput);
        String testData = """
                GET ws://localhost:8189/example HTTP/1.1
                Host: localhost:8189
                Connection: Upgrade
                Pragma: no-cache
                Cache-Control: no-cache
                User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36
                Upgrade: websocket
                Origin: http://localhost:8082
                Sec-WebSocket-Version: 13
                Accept-Encoding: gzip, deflate, br, zstd
                Accept-Language: en-US,en;q=0.9,vi-VN;q=0.8,vi;q=0.7
                Sec-WebSocket-Key: TixQkgsxKyup9IZVxSoe1w==
                Sec-WebSocket-Extensions: permessage-deflate; client_max_window_bits
                """;
        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(testData.getBytes()));
        when(mockSocket.isClosed()).thenReturn(true);

        // Run server
        serverRunner.run(mockServerSocket, executorService, running).get(0).get();


        // Verify server accepted a socket
        verify(mockServerSocket, atLeastOnce()).accept();
        verify(mockSocket, atLeastOnce()).getOutputStream();

        // Check that "Hello" was written
        String response = clientOutput.toString().trim();
        assertEquals(new StringBuilder().append("HTTP/1.1 101 Switching Protocols\r\n").append("Upgrade: websocket\r\n").append("Connection: Upgrade\r\n").append("Sec-WebSocket-Accept: O163+NfhFwxULDbPCuiQo7hGj30=").toString(), response);

        executorService.shutdownNow();
    }
}
