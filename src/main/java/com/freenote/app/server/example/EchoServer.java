package com.freenote.app.server.example;

import com.freenote.app.server.auth.impl.AcceptHandshakeImpl;
import com.freenote.app.server.parser.impl.HttpParserImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

import static com.freenote.app.server.startup.StartupProcess.HANDLERS;

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
        log.info("Serving incoming socket: {}", incomingSocket.getPort());

        var input = incomingSocket.getInputStream();
        var output = incomingSocket.getOutputStream();
        // Parse the HTTP upgrade request
        var request = new HttpParserImpl().parse(input);

        // Generate the upgrade response
        var response = new AcceptHandshakeImpl().handle(request);

        // Write response bytes (IMPORTANT: CRLF headers and blank line must be present)
        var responseBytes = response.toString().getBytes(StandardCharsets.UTF_8);
        log.info("Received response: {}", response.toString());

        output.write(responseBytes);
        output.flush();
//        log.info("socket state: {}", incomingSocket.isClosed());

        // DO NOT close the socket here — the WebSocket communication continues over it
        // Leave it open for later WebSocket frame communication

        while (!incomingSocket.isClosed()) {
            BiConsumer<InputStream, OutputStream> handler = HANDLERS.get(request.getPath())::handle;
            handler.accept(incomingSocket.getInputStream(), output);
        }
    }
}
