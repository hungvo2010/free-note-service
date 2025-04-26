package com.freenote.app.server.example;

import com.freenote.app.server.auth.impl.AcceptHandshakeImpl;
import com.freenote.app.server.parser.impl.HttpParserImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EchoServer {
    private static final Logger log = LogManager.getLogger(EchoServer.class);

    public static void main(String[] args) throws IOException {
        int port = args.length >= 1 ? Integer.parseInt(args[0]) : 8189;
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try (var s = new ServerSocket(port)) {
            while (true) {
                var incommingSocket = s.accept();
                executorService.submit(() -> {
                    try {
                        serve(incommingSocket);
                    } catch (IOException e) {
                        log.error("Failed to accept connection", e);
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    private static void serve(Socket incomingSocket) throws IOException {
        log.info("Serving incoming socket: {}", incomingSocket.getInetAddress().getHostName());

        var input = incomingSocket.getInputStream();
        var output = incomingSocket.getOutputStream();
        // Parse the HTTP upgrade request
        var request = new HttpParserImpl().parse(input);

        // Generate the upgrade response
        var response = new AcceptHandshakeImpl().handle(request);

        // Write response bytes (IMPORTANT: CRLF headers and blank line must be present)
        var responseBytes = response.toString().getBytes(StandardCharsets.UTF_8);
        log.info("Raw handshake response:\n" + response.toString().replace("\r", "\\r").replace("\n", "\\n\n"));

        output.write(responseBytes);
        output.flush();
        log.info("socket state: {}", incomingSocket.isClosed());

        // DO NOT close the socket here — the WebSocket communication continues over it
        // Leave it open for later WebSocket frame communication

        while (true) {
            int b = incomingSocket.getInputStream().read(); // blocking read bytes
            if (b == -1) break;
            log.info("Received byte: " + b);
        }
    }
}
