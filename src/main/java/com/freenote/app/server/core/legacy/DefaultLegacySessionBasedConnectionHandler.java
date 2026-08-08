package com.freenote.app.server.core.legacy;

import com.freenote.app.server.auth.AcceptHandshakeHandler;
import com.freenote.app.server.auth.impl.AcceptHandshakeImpl;
import com.freenote.app.server.core.model.connection.WebSocketConnection;
import com.freenote.app.server.core.model.connection.WebSocketSession;
import com.freenote.app.server.exceptions.AcceptConnectionException;
import com.freenote.app.server.exceptions.ClientDisconnectException;
import com.freenote.app.server.model.http.HttpUpgradeRequest;
import com.freenote.app.server.model.http.HttpUpgradeResponse;
import com.freenote.app.server.model.ws.NetworkRequestData;
import com.freenote.app.server.parser.HttpParser;
import com.freenote.app.server.parser.impl.HttpParserImpl;
import com.freenote.app.server.routes.URIEndpointHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import otel.metrics.MetricUtils;

import java.io.IOException;

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
        var upgradeRequest = parseRequest(session.getNetworkRequestData());
        var handShakeResp = performHandshake(upgradeRequest);
        session.sendHandshakeResponse(handShakeResp);

        routeToHandler(session, upgradeRequest);
    }

    private HttpUpgradeRequest parseRequest(NetworkRequestData networkRequestData) throws IOException {
        return httpParser.parse(networkRequestData.read());
    }

    private HttpUpgradeResponse performHandshake(HttpUpgradeRequest request) {
        log.debug("Performing handshake for: {}", request);
        var upgradeResponse = this.handshakeHandler.process(request);
        if (!upgradeResponse.getStatusCode().equals("101")) {
            throw new AcceptConnectionException("Handshake failed, connection not accepted");
        }

        return upgradeResponse;
    }

    private void routeToHandler(WebSocketSession session, HttpUpgradeRequest upgradeRequest) throws IOException {
        var pathHandler = getEndpointHandler(upgradeRequest);
        var outputWrapper = session.getOutputWrapper();
        MetricUtils.incrementConcurrentUsers();
        var networkRequestData = session.getNetworkRequestData();
        while (!networkRequestData.isClosed()) {
            pathHandler.handle(networkRequestData, outputWrapper);
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

    private void handleClientDisconnect(WebSocketSession session, Exception e) {
        log.error("Client disconnected => self closed: {}", e.getMessage());
        try {
            session.getNetworkRequestData().close();
        } catch (IOException ex) {
            log.error("Error closing connection", ex);
        }
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
            try {
                session.getNetworkRequestData().close();
            } catch (IOException ex) {
                log.error("Error closing connection", ex);
            }
        }
    }
}
