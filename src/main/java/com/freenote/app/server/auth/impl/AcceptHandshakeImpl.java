package com.freenote.app.server.auth.impl;

import com.freenote.app.server.auth.AcceptHandshakeHandler;
import com.freenote.app.server.exceptions.HandshakeException;
import com.freenote.app.server.http.HttpUpgradeRequest;
import com.freenote.app.server.http.HttpUpgradeResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Objects;

public class AcceptHandshakeImpl implements AcceptHandshakeHandler {
    private static final String UNIVERSAL_WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final Collection<String> DEFAULT_ALLOWED_ORIGINS = Arrays.asList(
            "http://localhost:3000",
            "http://localhost:63342",
            "http://localhost:8082",
            null,
            "http://localhost:8083",
            "http://localhost:5173",
            "http://localhost:8084",
            "http://localhost:8085",
            "http://localhost:8086",
            "http://localhost:8080"
    );
    private static Collection<String> ALLOWED_ORIGINS;
    private static final Logger log = LogManager.getLogger(AcceptHandshakeImpl.class);

    static {
        loadAllowedOriginsFromEnvs();
    }

    @Override
    public HttpUpgradeResponse handle(HttpUpgradeRequest request) {
        try {
            return acceptUpgradeRequest(request);
        } catch (Exception e) {
            return HttpUpgradeResponse.EMPTY_UPGRADE_RESPONSE;
        }
    }

    private HttpUpgradeResponse acceptUpgradeRequest(HttpUpgradeRequest request) {
        throwIfRejectHandshake(request);
        return buildApproveResponse(request);
    }

    private void throwIfRejectHandshake(HttpUpgradeRequest request) {
        var rejectHandShake = isRejectHandShake(request);
        if (rejectHandShake) {
            log.warn("Handshake not approved for request: {}", request);
            throw new HandshakeException(String.format("Handshake not approved for request: %s", request));
        }
    }

    private String getSocketAcceptKey(HttpUpgradeRequest request) {
        log.info("Received WebSocket upgrade request: {}", request);
        var socketAccept = generateAcceptKey(request);
        log.info("WebSocket handshake accepted with Sec-WebSocket-Accept: {}", socketAccept);
        return socketAccept;
    }

    private HttpUpgradeResponse buildApproveResponse(HttpUpgradeRequest request) {
        var socketAccept = getSocketAcceptKey(request);
        return HttpUpgradeResponse.builder()
                .statusCode("101")
                .statusText("Switching Protocols")
                .version("HTTP/1.1")
                .upgrade("websocket")
                .connection("Upgrade")
                .secWebSocketAccept(socketAccept)
                .httpUpgradeRequest(request)
                .build();
    }

    private static String generateAcceptKey(HttpUpgradeRequest request) {
        try {
            return Base64.getEncoder().encodeToString(
                    MessageDigest.getInstance("SHA-1")
                            .digest((request.getSecWebSocketKey() + UNIVERSAL_WEBSOCKET_GUID).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-1 algorithm not found for generating Sec-WebSocket-Accept key", e);
            throw new HandshakeException("SHA-1 algorithm not found for generating Sec-WebSocket-Accept key", e);
        }
    }

    private boolean isRejectHandShake(HttpUpgradeRequest request) {
        return Objects.isNull(request.getSecWebSocketKey())
                || Objects.isNull(request.getSecWebSocketVersion())
                || !ALLOWED_ORIGINS.contains(request.getOrigin());
    }

    private static void loadAllowedOriginsFromEnvs() {
        String additionalOrigins = System.getenv("ALLOWED_ORIGINS");
        if (additionalOrigins != null && !additionalOrigins.trim().isEmpty()) {
            Collection<String> combined = new java.util.ArrayList<>(DEFAULT_ALLOWED_ORIGINS);
            combined.addAll(Arrays.asList(additionalOrigins.split(",")));
            ALLOWED_ORIGINS = combined;
            log.info("Loaded additional allowed origins from environment: {}", additionalOrigins);
        } else {
            ALLOWED_ORIGINS = DEFAULT_ALLOWED_ORIGINS;
        }
        log.info("Allowed origins: {}", ALLOWED_ORIGINS);
    }
}
