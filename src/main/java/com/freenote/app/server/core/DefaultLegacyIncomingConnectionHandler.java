package com.freenote.app.server.core;

import com.freenote.app.server.auth.AcceptHandshakeHandler;
import com.freenote.app.server.auth.impl.AcceptHandshakeImpl;
import com.freenote.app.server.exceptions.AcceptConnectionException;
import com.freenote.app.server.exceptions.ClientDisconnectException;
import com.freenote.app.server.frames.factory.FrameFactory;
import com.freenote.app.server.handler.URIEndpointHandler;
import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.model.LegacyIOWrapper;
import com.freenote.app.server.model.OutputWrapper;
import com.freenote.app.server.model.TraceRequestData;
import com.freenote.app.server.model.http.HttpUpgradeRequest;
import com.freenote.app.server.model.http.HttpUpgradeResponse;
import com.freenote.app.server.model.ws.CommonRequestObject;
import com.freenote.app.server.parser.HttpParser;
import com.freenote.app.server.parser.impl.HttpParserImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;

import static generated.URIHandlerRegistry.getInstanceByURI;

public class DefaultLegacyIncomingConnectionHandler implements LegacyIncomingConnectionHandler {
    private static final Logger log = LogManager.getLogger(DefaultLegacyIncomingConnectionHandler.class);
    private final AcceptHandshakeHandler handshakeHandler;
    private final HttpParser httpParser;

    public DefaultLegacyIncomingConnectionHandler(AcceptHandshakeHandler handshakeHandler, HttpParser httpParser) {
        this.handshakeHandler = handshakeHandler;
        this.httpParser = httpParser;
    }

    public DefaultLegacyIncomingConnectionHandler() {
        this(new AcceptHandshakeImpl(), new HttpParserImpl());
    }

    @Override
    public void handle(LegacyIOWrapper legacyIOWrapper) throws IOException {
        try {
            serveConnection(legacyIOWrapper);
        } catch (ClientDisconnectException | AcceptConnectionException connectionException) {
            handleClientDisconnect(legacyIOWrapper, connectionException);
        } catch (Exception e) {
            handleError(legacyIOWrapper, e);
        }
    }

    private void serveConnection(LegacyIOWrapper legacyIOWrapper) throws IOException {
        var upgradeRequest = parseRequest(legacyIOWrapper);
        var handShakeResp = performHandshake(upgradeRequest);
        legacyIOWrapper.sendHandshakeResponse(handShakeResp);

        routeToHandler(legacyIOWrapper, upgradeRequest);
    }

    private HttpUpgradeRequest parseRequest(LegacyIOWrapper legacyIOWrapper) throws IOException {
        var socket = legacyIOWrapper.getSocket();
        return httpParser.parse(socket.getInputStream());
    }

    private HttpUpgradeResponse performHandshake(HttpUpgradeRequest request) {
        log.info("Performing handshake for: {}", request);
        var upgradeResponse = this.handshakeHandler.handle(request);
        if (!upgradeResponse.getStatusCode().equals("101")) {
            throw new AcceptConnectionException("Handshake failed, connection not accepted");
        }

        return upgradeResponse;
    }


    private void routeToHandler(LegacyIOWrapper legacyIOWrapper, HttpUpgradeRequest upgradeRequest) throws IOException {
        var socket = legacyIOWrapper.getSocket();
        var pathHandler = getEndpointHandler(upgradeRequest);
        var inputWrapper = buildInputWrapper(socket, upgradeRequest);
        var outputWrapper = new OutputWrapper(socket.getOutputStream());

        while (!socket.isClosed()) {
            pathHandler.handle(inputWrapper, outputWrapper);
        }
    }

    private URIEndpointHandler getEndpointHandler(HttpUpgradeRequest upgradeRequest) {
        var endpointHandler = (URIEndpointHandler) (getInstanceByURI(upgradeRequest.getPath()));
        if (endpointHandler == null) {
            log.warn("No handler found for URI: {}", upgradeRequest.getPath());
            throw new AcceptConnectionException("No handler for URI: " + upgradeRequest.getPath());
        }
        return endpointHandler;
    }

    private InputWrapper buildInputWrapper(Socket socket, HttpUpgradeRequest request) {
        var requestObject = CommonRequestObject.builder()
                .origin(request.getOrigin())
                .socket(socket)
                .build();
        requestObject.setRequestData(new TraceRequestData());

        var inputWrapper = new InputWrapper(socket);
        inputWrapper.setRequestObject(requestObject);

        return inputWrapper;
    }

    private void handleClientDisconnect(LegacyIOWrapper legacyIOWrapper, Exception e) throws IOException {
        log.error("Client disconnected => self closed: {}", e.getMessage());
        legacyIOWrapper.close();
    }

    private void handleError(LegacyIOWrapper socket, Exception e) {
        log.error("Error handling socket: ", e);
        try (socket) {
            socket.writeOutput(
                    FrameFactory.SERVER.createTextFrame("Internal Server Error"));
        } catch (Exception ignore) {
        }
    }
}
