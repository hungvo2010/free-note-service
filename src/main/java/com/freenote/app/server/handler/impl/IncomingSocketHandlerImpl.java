package com.freenote.app.server.handler.impl;

import com.freenote.app.server.application.factory.ApplicationFrameFactory;
import com.freenote.app.server.application.models.request.core.RequestObject;
import com.freenote.app.server.application.responses.InternalServerError;
import com.freenote.app.server.auth.impl.AcceptHandshakeImpl;
import com.freenote.app.server.exceptions.ClientDisconnectException;
import com.freenote.app.server.handler.IncomingConnectionHandler;
import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.http.HttpUpgradeRequest;
import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.parser.impl.HttpParserImpl;
import com.freenote.app.server.util.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static generated.URIHandlerRegistry.getInstanceByURI;

public class IncomingSocketHandlerImpl implements IncomingConnectionHandler {
    private static final Logger log = LogManager.getLogger(IncomingSocketHandlerImpl.class);

    @Override
    public void handle(Socket incomingSocket) throws IOException {
        var input = incomingSocket.getInputStream();
        var output = incomingSocket.getOutputStream();
        try {
            log.info("Serving incoming socket: {}", incomingSocket.getPort());


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
                URIHandler handler = pathHandler;
                handler.handle(inputWrapper, output);
            }
        } catch (ClientDisconnectException disconnectException) {
            log.error("Client disconnected => self closed ");
//            incomingSocket.close();
        } catch (Exception e) {
            log.error("Error handling socket: {}", e.getMessage());
            IOUtils.writeOutPut(
                    output,
                    ApplicationFrameFactory.SERVER.createApplicationFrame(new InternalServerError("Internal Server Error"))
            );
        }
    }

    private RequestObject handleHandShake(HttpUpgradeRequest request, OutputStream output) throws IOException {
        var response = new AcceptHandshakeImpl().handle(request);
        var responseBytes = response.toString().getBytes(StandardCharsets.UTF_8);
        output.write(responseBytes);
        output.flush();
        return RequestObject.builder()
                .origin(request.getOrigin())
                .build();
    }
}
