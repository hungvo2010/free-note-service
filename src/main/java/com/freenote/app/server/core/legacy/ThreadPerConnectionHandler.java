package com.freenote.app.server.core.legacy;

import com.freenote.app.server.auth.AcceptHandshakeHandler;
import com.freenote.app.server.auth.impl.AcceptHandshakeImpl;
import com.freenote.app.server.core.connection.IncomingConnectionHandler;
import com.freenote.app.server.core.context.ConnectionContext;
import com.freenote.app.server.core.model.connection.WebSocketConnection;
import com.freenote.app.server.exceptions.AcceptConnectionException;
import com.freenote.app.server.exceptions.ClientDisconnectException;
import com.freenote.app.server.model.OutputWrapper;
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

public class ThreadPerConnectionHandler implements IncomingConnectionHandler {
    private static final Logger log = LogManager.getLogger(ThreadPerConnectionHandler.class);
    private final AcceptHandshakeHandler handshakeHandler;
    private final HttpParser httpParser;

    public ThreadPerConnectionHandler(AcceptHandshakeHandler handshakeHandler, HttpParser httpParser) {
        this.handshakeHandler = handshakeHandler;
        this.httpParser = httpParser;
    }

    public ThreadPerConnectionHandler() {
        this(new AcceptHandshakeImpl(), new HttpParserImpl());
    }

    @Override
    public void handle(ConnectionContext context) {
        var networkRequestData = context.getNetworkRequestData();
        try {
            MetricUtils.incrementAcceptedHandshakeCount(1);
            doHandShakeAndRouting(networkRequestData);
        } catch (ClientDisconnectException | AcceptConnectionException connectionException) {
            MetricUtils.decrementConcurrentUsers();
            handleClientDisconnect(networkRequestData, connectionException);
        } catch (Exception e) {
            handleError(networkRequestData, e);
        }
    }

    private void doHandShakeAndRouting(NetworkRequestData networkRequestData) throws IOException {
        var upgradeRequest = parseRequest(networkRequestData);
        var handShakeResp = performHandshake(upgradeRequest);
        networkRequestData.write(handShakeResp.toRawBytes());

        routeToHandler(networkRequestData, upgradeRequest);
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

    private void routeToHandler(NetworkRequestData networkRequestData, HttpUpgradeRequest upgradeRequest) throws IOException {
        var pathHandler = getEndpointHandler(upgradeRequest);
        var outputWrapper = OutputWrapper.from(networkRequestData);
        MetricUtils.incrementConcurrentUsers();
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

    private void handleClientDisconnect(NetworkRequestData networkRequestData, Exception e) {
        log.error("Client disconnected => self closed: {}", e.getMessage());
        try {
            networkRequestData.close();
        } catch (IOException ex) {
            log.error("Error closing connection", ex);
        }
    }

    private void handleError(NetworkRequestData networkRequestData, Exception e) {
        log.error("Error handling socket: ", e);
        try {
            var context = WebSocketConnection.from(networkRequestData, OutputWrapper.from(networkRequestData));
            context.sendText("Internal Server Error");
            context.sendCurrentResponse();
        } catch (Exception ignore) {
        } finally {
            try {
                networkRequestData.close();
            } catch (IOException ex) {
                log.error("Error closing connection", ex);
            }
        }
    }
}