package com.freenote.app.server.core;

import com.freenote.app.server.auth.AcceptHandshakeHandler;
import com.freenote.app.server.auth.impl.AcceptHandshakeImpl;
import com.freenote.app.server.exceptions.AcceptConnectionException;
import com.freenote.app.server.exceptions.ClientDisconnectException;
import com.freenote.app.server.frames.factory.FrameFactory;
import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.http.HttpUpgradeRequest;
import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.model.TraceRequestData;
import com.freenote.app.server.model.ws.CommonRequestObject;
import com.freenote.app.server.parser.HttpParser;
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
    private final AcceptHandshakeHandler handleShakeHandler;
    private final HttpParser httpParser;

    public IncomingSocketHandlerImpl() {
        this.handleShakeHandler = new AcceptHandshakeImpl();
        this.httpParser = new HttpParserImpl();
    }

    @Override
    public void handle(Socket incomingSocket) throws IOException {
        var input = incomingSocket.getInputStream();
        var output = incomingSocket.getOutputStream();
        try {
            log.info("Serving incoming socket: {}", incomingSocket.getPort());
            var upgradeRequest = this.httpParser.parse(input);

            log.info("Received request: {}\n", upgradeRequest);
            doHandShake(upgradeRequest, output);

            var inputWrapper = buildInputWrapper(incomingSocket, upgradeRequest);

            while (!incomingSocket.isClosed()) { // todo: not correct due to incoming socket will not be closed after client disconnects
                var pathHandler = (URIHandler) (getInstanceByURI(upgradeRequest.getPath()));
                if (pathHandler == null) {
                    log.warn("No handler found for URI: {}", upgradeRequest.getPath());
                    return;
                }
                pathHandler.handle(inputWrapper, output);
            }
        } catch (ClientDisconnectException | AcceptConnectionException connectionException) {
            log.error("Client disconnected => self closed: {}", connectionException.getMessage());
            incomingSocket.close();
        } catch (Exception e) {
            log.error("Error handling socket: ", e);
            IOUtils.writeOutPut(
                    output,
                    FrameFactory.SERVER.createTextFrame("Internal Server Error")
            );
        }
    }

    private void doHandShake(HttpUpgradeRequest request, OutputStream output) throws IOException {
        var handShakeResp = this.handleShakeHandler.handle(request);
        output.write(handShakeResp.toString().getBytes(StandardCharsets.UTF_8));
        output.flush();
        if (!handShakeResp.getStatusCode().equals("101")) {
            throw new AcceptConnectionException("Handshake failed, connection not accepted");
        }
    }

    private InputWrapper buildInputWrapper(Socket incomingSocket, HttpUpgradeRequest request) {
        var requestObject = CommonRequestObject.builder()
                .origin(request.getOrigin())
                .socket(incomingSocket)
                .build();
        requestObject.setSocket(incomingSocket);
        requestObject.setRequestData(new TraceRequestData());

        var inputWrapper = new InputWrapper(incomingSocket);
        inputWrapper.setRequestObject(requestObject);

        return inputWrapper;
    }
}
