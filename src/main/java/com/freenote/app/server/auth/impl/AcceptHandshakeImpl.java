package com.freenote.app.server.auth.impl;

import com.freenote.app.server.auth.AcceptHandshakeHandler;
import com.freenote.app.server.http.HttpUpgradeRequest;
import com.freenote.app.server.http.HttpUpgradeResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class AcceptHandshakeImpl implements AcceptHandshakeHandler {
    private static final String UNIVERSAL_WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    @Override
    public HttpUpgradeResponse handle(HttpUpgradeRequest request) {
        try {
            var handShakeKey = request.getSecWebSocketKey();
            if (handShakeKey == null || handShakeKey.isEmpty()) {
                return HttpUpgradeResponse.EMPTY_UPGRADE_RESPONSE;
            }
            var socketAccept = Base64.getEncoder().encodeToString(
                    MessageDigest.getInstance("SHA-1").digest((handShakeKey + UNIVERSAL_WEBSOCKET_GUID).getBytes(StandardCharsets.UTF_8))
            );
            return HttpUpgradeResponse.builder()
                    .statusCode("101")
                    .statusText("Switching Protocols")
                    .version("HTTP/1.1")
                    .upgrade("websocket")
                    .connection("Upgrade")
                    .secWebSocketAccept(socketAccept)
                    .build();
        } catch (Exception e) {
            return new HttpUpgradeResponse();
        }
    }
}
