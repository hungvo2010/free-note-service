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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static com.freenote.app.server.startup.StartupProcess.HANDLERS;

public class EchoServer {
    private static final Logger log = LogManager.getLogger(EchoServer.class);

    public static void main(String[] args) throws IOException {
        int port = args.length >= 1 ? Integer.parseInt(args[0]) : 8189;
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try (var s = new ServerSocket(port)) {
            run(s, executorService, new AtomicBoolean(true));
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

        output.write(responseBytes);
        output.flush();

        while (!incomingSocket.isClosed()) {
            BiConsumer<InputStream, OutputStream> handler = HANDLERS.get(request.getPath())::handle;
            handler.accept(incomingSocket.getInputStream(), output);
        }
    }

    public static void run(ServerSocket serverSocket, ExecutorService executorService, AtomicBoolean running) throws IOException {
        while (running.get()) {
            var incomingSocket = serverSocket.accept();
            executorService.submit(() -> {
                try {
                    serve(incomingSocket);
                } catch (IOException e) {
                    log.error("Failed to accept connection", e);
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
