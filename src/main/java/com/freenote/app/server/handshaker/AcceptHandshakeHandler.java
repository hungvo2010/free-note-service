package com.freenote.app.server.handshaker;

import com.freenote.app.server.config.ConfigRepository;
import com.freenote.app.server.exceptions.WebSocketException;
import com.freenote.app.server.handshaker.http.HttpUgrade;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public interface AcceptHandshakeHandler {
    HttpUgrade.HttpUpgradeResponse process(HttpUgrade.HttpUpgradeRequest request);

    class AcceptHandshakeImpl implements AcceptHandshakeHandler {
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
                "http://localhost:8080",
                "https://free-note-ui.vercel.app",
                "https://home-laptop-server.tail71ae46.ts.net"
        );
        private static Collection<String> ALLOWED_ORIGINS;
        private static final Logger log = LogManager.getLogger(AcceptHandshakeImpl.class);

        static {
            loadAllowedOriginsFromConfig();
        }

        @Override
        public HttpUgrade.HttpUpgradeResponse process(HttpUgrade.HttpUpgradeRequest request) {
            try {
                return acceptUpgradeRequest(request);
            } catch (Exception e) {
                return HttpUgrade.HttpUpgradeResponse.EMPTY_UPGRADE_RESPONSE;
            }
        }

        private HttpUgrade.HttpUpgradeResponse acceptUpgradeRequest(HttpUgrade.HttpUpgradeRequest request) {
            throwIfRejectHandshake(request);
            return buildApproveResponse(request);
        }

        private void throwIfRejectHandshake(HttpUgrade.HttpUpgradeRequest request) {
            var rejectHandShake = isRejectHandShake(request);
            if (rejectHandShake) {
                log.warn("Handshake not approved for request: {}", request);
                throw new WebSocketException.HandshakeException(String.format("Handshake not approved for request: %s", request));
            }
        }

        private String getSocketAcceptKey(HttpUgrade.HttpUpgradeRequest request) {
            log.debug("Received WebSocket upgrade request: {}", request);
            var socketAccept = generateAcceptKey(request);
            log.debug("WebSocket handshake accepted with Sec-WebSocket-Accept: {}", socketAccept);
            return socketAccept;
        }

        private HttpUgrade.HttpUpgradeResponse buildApproveResponse(HttpUgrade.HttpUpgradeRequest request) {
            var socketAccept = getSocketAcceptKey(request);
            return HttpUgrade.HttpUpgradeResponse.builder()
                    .statusCode("101")
                    .statusText("Switching Protocols")
                    .version("HTTP/1.1")
                    .upgrade("websocket")
                    .connection("Upgrade")
                    .secWebSocketAccept(socketAccept)
                    .httpUpgradeRequest(request)
                    .build();
        }

        private static String generateAcceptKey(HttpUgrade.HttpUpgradeRequest request) {
            try {
                return Base64.getEncoder().encodeToString(
                        MessageDigest.getInstance("SHA-1")
                                .digest((request.getSecWebSocketKey() + UNIVERSAL_WEBSOCKET_GUID).getBytes(StandardCharsets.UTF_8)));
            } catch (NoSuchAlgorithmException e) {
                log.error("SHA-1 algorithm not found for generating Sec-WebSocket-Accept key", e);
                throw new WebSocketException.HandshakeException("SHA-1 algorithm not found for generating Sec-WebSocket-Accept key", e);
            }
        }

        private boolean isRejectHandShake(HttpUgrade.HttpUpgradeRequest request) {
            return Objects.isNull(request.getSecWebSocketKey())
                    || Objects.isNull(request.getSecWebSocketVersion())
                    || !ALLOWED_ORIGINS.contains(request.getOrigin());
        }

        private static void loadAllowedOriginsFromConfig() {
            var configRepo = new ConfigRepository();
            configRepo.load();
            String additionalOrigins = configRepo.getOrDefault("allowed.origins", "");
            var combined = new ArrayList<>(DEFAULT_ALLOWED_ORIGINS);
            if (isValidCommaSeparated(additionalOrigins)) {
                combined.addAll(Arrays.asList(additionalOrigins.split(",")));
                ALLOWED_ORIGINS = combined;
                log.info("Loaded additional allowed origins from application.properties: {}", additionalOrigins);
            } else {
                ALLOWED_ORIGINS = DEFAULT_ALLOWED_ORIGINS;
            }
            log.debug("Allowed origins: {}", ALLOWED_ORIGINS);
        }

        private static boolean isValidCommaSeparated(String additionalOrigins) {
            return additionalOrigins != null && !additionalOrigins.trim().isEmpty();
        }
    }
}
