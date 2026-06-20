package com.freenote.app.server.core.nio;

import com.freenote.app.server.auth.AcceptHandshakeHandler;
import com.freenote.app.server.auth.impl.AcceptHandshakeImpl;
import com.freenote.app.server.core.connection.IncomingConnectionHandler;
import com.freenote.app.server.core.context.ConnectionContext;
import com.freenote.app.server.core.context.ReadableContext;
import com.freenote.app.server.exceptions.AcceptConnectionException;
import com.freenote.app.server.exceptions.ConnectionException;
import com.freenote.app.server.model.OutputWrapper;
import com.freenote.app.server.model.http.HttpUpgradeRequest;
import com.freenote.app.server.model.ws.NetworkRequestData;
import com.freenote.app.server.parser.HttpParser;
import com.freenote.app.server.parser.impl.HttpParserImpl;
import com.freenote.app.server.routes.URIEndpointHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static generated.URIHandlerRegistry.getInstanceByURI;

public class NIOIncomingSocketHandler implements IncomingConnectionHandler {
    private static final Logger log = LogManager.getLogger(NIOIncomingSocketHandler.class);
    private final AcceptHandshakeHandler handshakeHandler;
    private final HttpParser httpParser;

    public NIOIncomingSocketHandler(AcceptHandshakeHandler handshakeHandler, HttpParser httpParser) {
        this.handshakeHandler = handshakeHandler;
        this.httpParser = httpParser;
    }

    public NIOIncomingSocketHandler() {
        this(new AcceptHandshakeImpl(), new HttpParserImpl());
    }

    private void performHandshake(NetworkRequestData networkData, HttpUpgradeRequest request) throws IOException {
        log.info("Performing handshake for: {}", request);
        var handShakeResp = this.handshakeHandler.process(request);
        var outputBytes = handShakeResp.toString().getBytes(StandardCharsets.UTF_8);
        networkData.write(outputBytes);
    }

    private void routeToHandler(NetworkRequestData networkData, HttpUpgradeRequest upgradeRequest) throws IOException {
        var pathHandler = getPathHandler(upgradeRequest);
        var outputWrapper = OutputWrapper.from(networkData);
        pathHandler.handle(networkData, outputWrapper);
    }

    private URIEndpointHandler getPathHandler(HttpUpgradeRequest upgradeRequest) {
        var pathHandler = (URIEndpointHandler) (getInstanceByURI(upgradeRequest.getPath()));
        if (pathHandler == null) {
            log.warn("No handler found for URI: {}", upgradeRequest.getPath());
            throw new AcceptConnectionException("No handler for URI: " + upgradeRequest.getPath());
        }
        return pathHandler;
    }

    @Override
    public void handle(ConnectionContext context) throws ConnectionException {
        var networkData = context.getNetworkRequestData();
        var readableContext = context.getReadableContext();

        if (readableContext.isHandshakeComplete()) {
            processMessage(networkData, readableContext.getHttpUpgradeRequest());
        } else {
            acceptHandshake(networkData, readableContext);
        }
    }

    private void acceptHandshake(NetworkRequestData networkData, ReadableContext readableContext) {
        try {
            var upgradeRequest = httpParser.parse(networkData.read());
            performHandshake(networkData, upgradeRequest);
            readableContext.setHttpUpgradeRequest(upgradeRequest);
        } catch (IOException e) {
            log.error("Error during handshake", e);
            closeQuietly(networkData);
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
