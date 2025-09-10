package com.freenote.app.server.auth.impl;

import com.freenote.app.server.auth.AcceptHandshakeHandler;
import com.freenote.app.server.http.HttpUpgradeRequest;
import com.freenote.app.server.http.HttpUpgradeResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Objects;

public class AcceptHandshakeImpl implements AcceptHandshakeHandler {
    private static final String UNIVERSAL_WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final Collection<String> ALLOWED_ORIGINS = Arrays.asList(
            "http://localhost:3000",
            "",
            null,
            "http://localhost:63342",
            "http://localhost:8082",
            "http://localhost:8083",
            "http://localhost:8084",
            "http://localhost:8085",
            "http://localhost:8086",
            "http://localhost:8080"
    );
    private static final Logger log = LogManager.getLogger(AcceptHandshakeImpl.class);

    @Override
    public HttpUpgradeResponse handle(HttpUpgradeRequest request) {
        try {
            var handshakeApproved = checkHandshakeApproval(request);
            if (!handshakeApproved) {
                log.warn("Handshake not approved for request: {}", request);
                return HttpUpgradeResponse.EMPTY_UPGRADE_RESPONSE;
            }
            log.info("Received WebSocket upgrade request: {}", request);
            var socketAccept = Base64.getEncoder().encodeToString(
                    MessageDigest.getInstance("SHA-1")
                            .digest((request.getSecWebSocketKey() + UNIVERSAL_WEBSOCKET_GUID).getBytes(StandardCharsets.UTF_8)));
            return HttpUpgradeResponse.builder().statusCode("101").statusText("Switching Protocols").version("HTTP/1.1").upgrade("websocket").connection("Upgrade").secWebSocketAccept(socketAccept).build();
        } catch (Exception e) {
            return new HttpUpgradeResponse();
        }
    }

    private boolean checkHandshakeApproval(HttpUpgradeRequest request) {
        if (!ALLOWED_ORIGINS.contains(request.getOrigin())) {
            return false;
        }
        return !(Objects.isNull(request.getSecWebSocketKey())
//                || Objects.isNull(request.getSecWebSocketExtensions())
                || Objects.isNull(request.getSecWebSocketVersion()));
    }
}
