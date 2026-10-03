package com.freenote.app.server.core.nio;

import com.freenote.app.server.core.nio.state.ConnectionState;
import com.freenote.app.server.exceptions.WebSocketException;
import com.freenote.app.server.handshaker.AcceptHandshakeHandler;
import com.freenote.app.server.core.connection.PerClientConnectionHandler;
import com.freenote.app.server.core.event.ConnectionEvent;
import com.freenote.app.server.endpoints.EndpointResolver;
import com.freenote.app.server.handshaker.http.HttpUgrade;
import com.freenote.app.server.model.ws.NetworkResponseData;
import com.freenote.app.server.model.ws.NetworkRequestData;
import com.freenote.app.server.parser.HttpParser;
import com.freenote.app.server.endpoints.URIEndpointHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class NIOIncomingSocketHandler implements PerClientConnectionHandler {
    private static final Logger log = LogManager.getLogger(NIOIncomingSocketHandler.class);
    private final AcceptHandshakeHandler handshakeHandler;
    private final HttpParser httpParser;
    private final EndpointResolver endpointResolver;

    public NIOIncomingSocketHandler(AcceptHandshakeHandler handshakeHandler, HttpParser httpParser, EndpointResolver endpointResolver) {
        this.handshakeHandler = handshakeHandler;
        this.httpParser = httpParser;
        this.endpointResolver = endpointResolver;
    }

    public NIOIncomingSocketHandler(EndpointResolver endpointResolver) {
        this(new AcceptHandshakeHandler.AcceptHandshakeImpl(), new HttpParser.HttpParserImpl(), endpointResolver);
    }

    private void writeHandshakeResponse(NetworkRequestData networkData, HttpUgrade.HttpUpgradeRequest request) throws IOException {
        log.info("Performing handshake for: {}", request);
        var handShakeResp = this.handshakeHandler.process(request);
        var outputBytes = handShakeResp.toString().getBytes(StandardCharsets.UTF_8);
        networkData.write(outputBytes);
    }

    private void routeToHandler(NetworkRequestData networkData, HttpUgrade.HttpUpgradeRequest upgradeRequest) throws IOException {
        var pathHandler = getPathHandler(upgradeRequest);
        var responseData = NetworkResponseData.from(networkData);
        pathHandler.handle(networkData, responseData);
    }

    private URIEndpointHandler getPathHandler(HttpUgrade.HttpUpgradeRequest upgradeRequest) {
        var pathHandler = endpointResolver.resolve(upgradeRequest.getPath());
        if (pathHandler == null) {
            log.warn("No handler found for URI: {}", upgradeRequest.getPath());
            throw new WebSocketException.AcceptConnectionException("No handler for URI: " + upgradeRequest.getPath());
        }
        return pathHandler;
    }

    @Override
    public HttpUgrade.HttpUpgradeRequest handle(ConnectionEvent event) throws WebSocketException.ConnectionException {
        var networkData = event.getNetworkRequestData();

        if (event.getState() instanceof ConnectionState.MessageState messageState) {
            processMessage(networkData, messageState.getRequest());
            return null;
        }
        return acceptHandshake(networkData);
    }

    private HttpUgrade.HttpUpgradeRequest acceptHandshake(NetworkRequestData networkData) {
        try {
            var upgradeRequest = httpParser.parse(networkData.read());
            writeHandshakeResponse(networkData, upgradeRequest);
            return upgradeRequest;
        } catch (IOException e) {
            log.error("Error during handshake", e);
            closeQuietly(networkData);
            return null;
        }
    }

    private void processMessage(NetworkRequestData networkData, HttpUgrade.HttpUpgradeRequest upgradeRequest) {
        try {
            routeToHandler(networkData, upgradeRequest);
        } catch (IOException e) {
            log.error("Error routing message", e);
            closeQuietly(networkData);
        }
    }

    private void closeQuietly(NetworkRequestData networkData) {
        try {
            networkData.close();
        } catch (IOException ex) {
            log.error("Error closing connection", ex);
        }
    }
}
