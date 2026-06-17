package com.freenote.app.server.core.legacy;

import com.freenote.app.server.auth.AcceptHandshakeHandler;
import com.freenote.app.server.auth.impl.AcceptHandshakeImpl;
import com.freenote.app.server.core.connection.WebSocketConnection;
import com.freenote.app.server.core.connection.WebSocketSession;
import com.freenote.app.server.exceptions.AcceptConnectionException;
import com.freenote.app.server.exceptions.ClientDisconnectException;
import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.model.http.HttpUpgradeRequest;
import com.freenote.app.server.model.http.HttpUpgradeResponse;
import com.freenote.app.server.model.ws.AppRequestData;
import com.freenote.app.server.parser.HttpParser;
import com.freenote.app.server.parser.impl.HttpParserImpl;
import com.freenote.app.server.routes.URIEndpointHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import otel.metrics.MetricUtils;

import java.io.IOException;
import java.net.Socket;

import static generated.URIHandlerRegistry.getInstanceByURI;

public class DefaultLegacySessionBasedConnectionHandler implements LegacySessionBasedConnectionHandler {
    private static final Logger log = LogManager.getLogger(DefaultLegacySessionBasedConnectionHandler.class);
    private final AcceptHandshakeHandler handshakeHandler;
    private final HttpParser httpParser;

    public DefaultLegacySessionBasedConnectionHandler(AcceptHandshakeHandler handshakeHandler, HttpParser httpParser) {
        this.handshakeHandler = handshakeHandler;
        this.httpParser = httpParser;
    }

    public DefaultLegacySessionBasedConnectionHandler() {
        this(new AcceptHandshakeImpl(), new HttpParserImpl());
    }

    @Override
    public void handle(WebSocketSession session) throws IOException {
        try {
            MetricUtils.incrementAcceptedHandshakeCount(1);
            doHandShakeAndRouting(session);
        } catch (ClientDisconnectException | AcceptConnectionException connectionException) {
            MetricUtils.decrementConcurrentUsers();
            handleClientDisconnect(session, connectionException);
        } catch (Exception e) {
            handleError(session, e);
        }
    }

    private void doHandShakeAndRouting(WebSocketSession session) throws IOException {
        var upgradeRequest = parseRequest(session);
        var handShakeResp = performHandshake(upgradeRequest);
        session.sendHandshakeResponse(handShakeResp);

        routeToHandler(session, upgradeRequest);
    }

    private HttpUpgradeRequest parseRequest(WebSocketSession session) throws IOException {
        var socket = session.getSocket();
        return httpParser.parse(socket.getInputStream());
    }

    private HttpUpgradeResponse performHandshake(HttpUpgradeRequest request) {
        log.info("Performing handshake for: {}", request);
        var upgradeResponse = this.handshakeHandler.process(request);
        if (!upgradeResponse.getStatusCode().equals("101")) {
            throw new AcceptConnectionException("Handshake failed, connection not accepted");
        }

        return upgradeResponse;
    }


    private void routeToHandler(WebSocketSession session, HttpUpgradeRequest upgradeRequest) throws IOException {
        var socket = session.getSocket();
        var pathHandler = getEndpointHandler(upgradeRequest);
        var inputWrapper = buildInputWrapper(socket, upgradeRequest);
        var outputWrapper = session.getOutputWrapper();
        MetricUtils.incrementConcurrentUsers();
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
        var appRequestData = AppRequestData.builder()
                .requestOrigin(request.getOrigin())
                .build();

        var inputWrapper = new InputWrapper(socket);
        inputWrapper.setAppRequestData(appRequestData);
        inputWrapper.setSocket(socket);

        return inputWrapper;
    }

    private void handleClientDisconnect(WebSocketSession session, Exception e) throws IOException {
        log.error("Client disconnected => self closed: {}", e.getMessage());
        closeSocket(session.getSocket());
    }

    private void handleError(WebSocketSession session, Exception e) {
        log.error("Error handling socket: ", e);
        try {
            var context = WebSocketConnection.builder()
                    .session(session)
                    .build();
            context.sendText("Internal Server Error");
            context.sendCurrentResponse();
        } catch (Exception ignore) {
        } finally {
            closeSocket(session.getSocket());
        }
    }

    private void closeSocket(Socket socket) {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                log.error("Error closing socket", e);
            }
        }
    }
}
