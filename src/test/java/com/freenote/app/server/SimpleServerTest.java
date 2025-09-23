package com.freenote.app.server;

import com.freenote.app.server.example.SimpleServer;
import org.junit.jupiter.api.Test;

import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimpleServerTest {
    @Test
    void testMainRunsInBackground() throws Exception {
        Thread t = new Thread(() -> {
            try {
                SimpleServer.main(new String[]{"9091"});
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        t.start();

        // Wait briefly
        Thread.sleep(500);

        // Try connecting to confirm server is up
        try (Socket socket = new Socket("localhost", 9091)) {
            assertTrue(socket.isConnected());
        }

        // Stop the server gracefully
        t.interrupt();
    }
}
