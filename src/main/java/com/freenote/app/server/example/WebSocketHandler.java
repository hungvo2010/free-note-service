package com.freenote.app.server.example;

import com.freenote.app.server.auth.impl.AcceptHandshakeImpl;
import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.http.HttpUpgradeRequest;
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
        handleHandShake(request, output);

        while (!incomingSocket.isClosed()) {
            var pathHandler = (URIHandler) (getInstanceByURI(request.getPath()));
            if (pathHandler == null) {
                log.warn("No handler found for URI: {}", request.getPath());
                return;
            }
            BiConsumer<InputStream, OutputStream> handler = (pathHandler)::handle;
            handler.accept(input, output);
        }

        log.info("Closing socket: {}", incomingSocket.getPort());
    }

    private static void handleHandShake(HttpUpgradeRequest request, OutputStream output) throws IOException {
        var response = new AcceptHandshakeImpl().handle(request);
        var responseBytes = response.toString().getBytes(StandardCharsets.UTF_8);
        output.write(responseBytes);
        output.flush();
    }
}
