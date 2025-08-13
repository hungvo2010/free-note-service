package com.freenote.app.server.example;

import com.freenote.app.server.annotations.WebSocketServer;
import com.freenote.app.server.auth.impl.AcceptHandshakeImpl;
import com.freenote.app.server.context.ApplicationContext;
import com.freenote.app.server.parser.impl.HttpParserImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

@WebSocketServer
public class SimpleServer {
    private static final Logger log = LogManager.getLogger(SimpleServer.class);

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
        var request = new HttpParserImpl().parse(input);

        log.info("Received request: {}\n", request);

        var response = new AcceptHandshakeImpl().handle(request);
        var responseBytes = response.toString().getBytes(StandardCharsets.UTF_8);
        output.write(responseBytes);
        output.flush();

        while (!incomingSocket.isClosed()) {
            BiConsumer<InputStream, OutputStream> handler = ApplicationContext.getHandlers().get(request.getPath())::handle;
            handler.accept(input, output);
        }
    }

    public static List<Future> run(ServerSocket serverSocket, ExecutorService executorService, AtomicBoolean running) throws IOException {
        List<Future> futures = new ArrayList<>();
        while (running.get()) {
            var incomingSocket = serverSocket.accept();
            futures.add(executorService.submit(() -> {
                try {
                    serve(incomingSocket);
                } catch (IOException e) {
                    log.error("Failed to accept connection", e);
                    throw new RuntimeException(e);
                }
            }));
        }
        return futures;
    }
}
