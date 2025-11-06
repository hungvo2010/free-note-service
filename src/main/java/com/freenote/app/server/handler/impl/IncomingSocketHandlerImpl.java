package com.freenote.app.server.handler.impl;

import com.freenote.app.server.application.models.request.core.RequestObject;
import com.freenote.app.server.auth.impl.AcceptHandshakeImpl;
import com.freenote.app.server.handler.IncomingConnectionHandler;
import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.http.HttpUpgradeRequest;
import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.parser.impl.HttpParserImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

import static generated.URIHandlerRegistry.getInstanceByURI;

public class IncomingSocketHandlerImpl implements IncomingConnectionHandler {
    private static final Logger log = LogManager.getLogger(IncomingSocketHandlerImpl.class);

    @Override
    public void handle(Socket incomingSocket) throws IOException {
        try {
            log.info("Serving incoming socket: {}", incomingSocket.getPort());

            var input = incomingSocket.getInputStream();
            var output = incomingSocket.getOutputStream();
            var request = new HttpParserImpl().parse(input);

            log.info("Received request: {}\n", request);
            var requestObject = handleHandShake(request, output);
            var inputWrapper = new InputWrapper(input, incomingSocket);
            requestObject.setSocket(incomingSocket);
            inputWrapper.setRequestObject(requestObject);

            while (!incomingSocket.isClosed()) { // todo: not correct due to incoming socket will not be closed after client disconnects
                var pathHandler = (URIHandler) (getInstanceByURI(request.getPath()));
                if (pathHandler == null) {
                    log.warn("No handler found for URI: {}", request.getPath());
                    return;
                }
                BiConsumer<InputWrapper, OutputStream> handler = (pathHandler)::handle;
                handler.accept(inputWrapper, output);
            }
        } catch (Exception e) {
            log.error("Error handling socket: {}", e.getMessage());

        } finally {

            log.info("Closing socket: {}", incomingSocket.getPort());
            incomingSocket.close();
        }
    }

    private RequestObject handleHandShake(HttpUpgradeRequest request, OutputStream output) throws IOException {
        var response = new AcceptHandshakeImpl().handle(request);
        var responseBytes = response.toString().getBytes(StandardCharsets.UTF_8);
        output.write(responseBytes);
        output.flush();
        var requestObject = RequestObject.builder()
                .origin(request.getOrigin())
                .build();
        return requestObject;
    }
}
