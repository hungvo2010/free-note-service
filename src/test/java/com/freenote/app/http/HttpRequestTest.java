package com.freenote.app.http;

import com.freenote.app.server.handshaker.http.HttpUgrade;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class HttpRequestTest {
    @Test
    void givenSpecialCaseThenSuccess() {
        var httpRequest = HttpUgrade.HttpUpgradeRequest.builder()
                .secWebSocketKey(null)
                .secWebSocketVersion(null)
                .build();
        assertSame(HttpUgrade.HttpUpgradeRequest.EMPTY_UPGRADE_REQUEST, httpRequest);
    }

    @Test
    void givenSpecialCaseThenSuccess1() {
        var httpRequest = HttpUgrade.HttpUpgradeRequest.builder()
                .secWebSocketKey("testkey")
                .secWebSocketVersion(null)
                .build();
        assertSame(HttpUgrade.HttpUpgradeRequest.EMPTY_UPGRADE_REQUEST, httpRequest);
    }

    @Test
    void givenSpecialCaseThenSuccess2() {
        var httpRequest = HttpUgrade.HttpUpgradeRequest.builder()
                .secWebSocketKey(null)
                .secWebSocketVersion("testversion")
                .build();
        assertSame(HttpUgrade.HttpUpgradeRequest.EMPTY_UPGRADE_REQUEST, httpRequest);
    }

    @Test
    void givenSpecialCaseThenSuccess3() {
        var httpRequest = HttpUgrade.HttpUpgradeRequest.builder()
                .secWebSocketKey("")
                .secWebSocketVersion(null)
                .build();
        assertSame(HttpUgrade.HttpUpgradeRequest.EMPTY_UPGRADE_REQUEST, httpRequest);
    }

    @Test
    void givenSpecialCaseThenSuccess4() {
        var httpRequest = HttpUgrade.HttpUpgradeRequest.builder()
                .secWebSocketKey(null)
                .secWebSocketVersion("")
                .build();
        assertSame(HttpUgrade.HttpUpgradeRequest.EMPTY_UPGRADE_REQUEST, httpRequest);
    }

    @Test
    void givenSpecialCaseThenSuccess5() {
        var httpRequest = HttpUgrade.HttpUpgradeRequest.builder()
                .secWebSocketKey("")
                .secWebSocketVersion("facebook")
                .build();
        assertSame(HttpUgrade.HttpUpgradeRequest.EMPTY_UPGRADE_REQUEST, httpRequest);
    }

    @Test
    void givenSpecialCaseThenSuccess6() {
        var httpRequest = HttpUgrade.HttpUpgradeRequest.builder()
                .secWebSocketKey("null")
                .secWebSocketVersion("")
                .build();
        assertSame(HttpUgrade.HttpUpgradeRequest.EMPTY_UPGRADE_REQUEST, httpRequest);
    }

    @Test
    void givenNormalParams_thenCallBuilder_Success() {
        var httpRequest = HttpUgrade.HttpUpgradeRequest.builder()
                .uri("ws://localhost:8080/chat")
                .secWebSocketKey("test-key")
                .secWebSocketVersion("13")
                .build();
        assertEquals("test-key", httpRequest.getSecWebSocketKey());
        assertEquals("13", httpRequest.getSecWebSocketVersion());
        assertEquals("ws://localhost:8080/chat", httpRequest.getUri());
        assertEquals("/chat", httpRequest.getPath());
    }
}
