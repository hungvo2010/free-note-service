package com.freenote.app.server.core.legacy;

import com.freenote.app.server.exceptions.WebSocketException;
import com.freenote.app.server.handshaker.AcceptHandshakeHandler;
import com.freenote.app.server.core.connection.PerClientConnectionHandler;
import com.freenote.app.server.core.event.ConnectionEvent;
import com.freenote.app.server.endpoints.EndpointResolver;
import com.freenote.app.server.model.connection.WebSocketConnection;
import com.freenote.app.server.handshaker.http.HttpUgrade;
import com.freenote.app.server.model.ws.NetworkResponseData;
import com.freenote.app.server.model.ws.NetworkRequestData;
import com.freenote.app.server.parser.HttpParser;
import com.freenote.app.server.endpoints.URIEndpointHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import otel.metrics.MetricUtils;

import java.io.IOException;

public class ThreadPerConnectionHandler implements PerClientConnectionHandler {
    private static final Logger log = LogManager.getLogger(ThreadPerConnectionHandler.class);
    private final AcceptHandshakeHandler handshakeHandler;
    private final HttpParser httpParser;
    private final EndpointResolver endpointResolver;

    public ThreadPerConnectionHandler(AcceptHandshakeHandler handshakeHandler, HttpParser httpParser, EndpointResolver endpointResolver) {
        this.handshakeHandler = handshakeHandler;
        this.httpParser = httpParser;
        this.endpointResolver = endpointResolver;
    }

    public ThreadPerConnectionHandler(EndpointResolver endpointResolver) {
        this(new AcceptHandshakeHandler.AcceptHandshakeImpl(), new HttpParser.HttpParserImpl(), endpointResolver);
    }

    @Override
    public HttpUgrade.HttpUpgradeRequest handle(ConnectionEvent event) throws WebSocketException.ConnectionException, IOException {
        var networkRequestData = event.getNetworkRequestData();
        try {
            var upgradeRequest = doHandShakeAndRouting(networkRequestData);
            pollToEndpointHandler(networkRequestData, upgradeRequest);
        } catch (WebSocketException.ClientDisconnectException |
                 WebSocketException.AcceptConnectionException connectionException) {
            // concurrent-users đã được decrement trong routeToHandler#finally
            log.info("Client disconnected => self closed: {}", connectionException.getMessage());
            closeRequest(networkRequestData);
        } catch (Exception e) {
            log.error("Error handling socket: ", e);
            handleError(networkRequestData);
        }
        return null;
    }

    private HttpUgrade.HttpUpgradeRequest doHandShakeAndRouting(NetworkRequestData networkRequestData) throws IOException {
        var upgradeRequest = httpParser.parse(networkRequestData.read());
        performHandshake(networkRequestData, upgradeRequest);
        MetricUtils.incrementAcceptedHandshakeCount(1);
        return upgradeRequest;
    }

    private void performHandshake(NetworkRequestData networkRequestData, HttpUgrade.HttpUpgradeRequest request) throws IOException {
        log.debug("Performing handshake for: {}", request);
        var upgradeResponse = this.handshakeHandler.process(request);
        if (!upgradeResponse.getStatusCode().equals("101")) {
            throw new WebSocketException.AcceptConnectionException("Handshake failed, connection not accepted");
        }

        networkRequestData.write(upgradeResponse.toRawBytes());
    }

    private void pollToEndpointHandler(NetworkRequestData networkRequestData, HttpUgrade.HttpUpgradeRequest upgradeRequest) throws IOException {
        var endpointHandler = getEndpointHandler(upgradeRequest);
        var responseData = NetworkResponseData.from(networkRequestData);
        MetricUtils.incrementConcurrentUsers();
        try {
            while (!networkRequestData.isClosed()) {
                // handle() trả false khi client đã ngắt / hết dữ liệu -> PHẢI thoát vòng lặp.
                // Không dựa vào socket.isClosed(): với SSLSocket sau close_notify nó vẫn false,
                // nên trước đây vòng lặp quay nóng + spam log vô hạn cho tới khi hết đĩa.
                if (!endpointHandler.handle(networkRequestData, responseData)) {
                    log.info("Client disconnected, stopping read loop for {}", networkRequestData.getRemoteAddress());
                    break;
                }
            }
        } finally {
            MetricUtils.decrementConcurrentUsers();
            closeRequest(networkRequestData);
        }
    }

    private URIEndpointHandler getEndpointHandler(HttpUgrade.HttpUpgradeRequest upgradeRequest) {
        var endpointHandler = endpointResolver.resolve(upgradeRequest.getPath());
        if (endpointHandler == null) {
            log.error("No handler found for URI: {}", upgradeRequest.getPath());
            throw new WebSocketException.AcceptConnectionException("No handler for URI: " + upgradeRequest.getPath());
        }
        return endpointHandler;
    }

    private void handleError(NetworkRequestData networkRequestData) {
        try {
            var context = WebSocketConnection.from(networkRequestData, NetworkResponseData.from(networkRequestData));
            context.sendText("Internal Server Error");
            context.sendCurrentResponse();
        } catch (Exception ignore) {
        } finally {
            closeRequest(networkRequestData);
        }
    }

    private void closeRequest(NetworkRequestData networkRequestData) {
        try {
            networkRequestData.close();
        } catch (IOException e) {
            throw new WebSocketException.ConnectionException("Error closing connection", e);
        }
    }
}
