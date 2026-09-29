package com.freenote.app.server.core.nio;

import com.freenote.app.server.handshaker.AcceptHandshakeHandler;
import com.freenote.app.server.handshaker.impl.AcceptHandshakeImpl;
import com.freenote.app.server.core.connection.PerClientConnectionHandler;
import com.freenote.app.server.core.event.ConnectionEvent;
import com.freenote.app.server.core.nio.state.MessageState;
import com.freenote.app.server.endpoints.EndpointResolver;
import com.freenote.app.server.exceptions.AcceptConnectionException;
import com.freenote.app.server.exceptions.ConnectionException;
import com.freenote.app.server.model.ws.NetworkResponseData;
import com.freenote.app.server.model.http.HttpUpgradeRequest;
import com.freenote.app.server.model.ws.NetworkRequestData;
import com.freenote.app.server.parser.HttpParser;
import com.freenote.app.server.parser.impl.HttpParserImpl;
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
        this(new AcceptHandshakeImpl(), new HttpParserImpl(), endpointResolver);
    }

    private void writeHandshakeResponse(NetworkRequestData networkData, HttpUpgradeRequest request) throws IOException {
        log.info("Performing handshake for: {}", request);
        var handShakeResp = this.handshakeHandler.process(request);
        var outputBytes = handShakeResp.toString().getBytes(StandardCharsets.UTF_8);
        networkData.write(outputBytes);
    }

    private void routeToHandler(NetworkRequestData networkData, HttpUpgradeRequest upgradeRequest) throws IOException {
        var pathHandler = getPathHandler(upgradeRequest);
        var responseData = NetworkResponseData.from(networkData);
        pathHandler.handle(networkData, responseData);
    }

    private URIEndpointHandler getPathHandler(HttpUpgradeRequest upgradeRequest) {
        var pathHandler = endpointResolver.resolve(upgradeRequest.getPath());
        if (pathHandler == null) {
            log.warn("No handler found for URI: {}", upgradeRequest.getPath());
            throw new AcceptConnectionException("No handler for URI: " + upgradeRequest.getPath());
        }
        return pathHandler;
    }

    @Override
    public HttpUpgradeRequest handle(ConnectionEvent event) throws ConnectionException {
        var networkData = event.getNetworkRequestData();

        if (event.getState() instanceof MessageState messageState) {
            processMessage(networkData, messageState.getRequest());
            return null;
        }
        return acceptHandshake(networkData);
    }

    private HttpUpgradeRequest acceptHandshake(NetworkRequestData networkData) {
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

    private void processMessage(NetworkRequestData networkData, HttpUpgradeRequest upgradeRequest) {
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
