package com.freenote.app.server;

import com.freenote.app.server.example.EchoServer;
import org.junit.jupiter.api.Test;

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
        EchoServer serverRunner = new EchoServer();

        // Simulate accepting one connection then stop
        when(mockServerSocket.accept()).thenAnswer(invocation -> {
            running.set(false); // Accept once then stop server
            return mockSocket;
        });

        // Capture output written to client socket
        ByteArrayOutputStream clientOutput = new ByteArrayOutputStream();
        when(mockSocket.getOutputStream()).thenReturn(clientOutput);

        // Run server
        serverRunner.run(mockServerSocket, executorService, running);

        // Verify server accepted a socket
        verify(mockServerSocket, atLeastOnce()).accept();
        verify(mockSocket, atLeastOnce()).getOutputStream();

        // Check that "Hello" was written
        String response = clientOutput.toString().trim();
        assertEquals("Hello", response);

        executorService.shutdownNow();
    }
}
