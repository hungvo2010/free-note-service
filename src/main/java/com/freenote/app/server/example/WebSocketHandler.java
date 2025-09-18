package com.freenote.app.server.example;

import com.freenote.app.server.auth.impl.AcceptHandshakeImpl;
import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.parser.impl.HttpParserImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

import static generated.URIHandlerRegistry.getInstanceByURI;

public class WebSocketHandler implements ConnectionHandler {
    private static final Logger log = LogManager.getLogger(WebSocketHandler.class);

    @Override
    public void handle(Socket incomingSocket) throws IOException {
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
            log.info(incomingSocket.getClass().getName());
            log.info("Waiting for next message...");
            BiConsumer<InputStream, OutputStream> handler = ((URIHandler) (getInstanceByURI(request.getPath())))::handle;
            handler.accept(input, output);
        }

        log.info("Closing socket: {}", incomingSocket.getPort());
    }
}
