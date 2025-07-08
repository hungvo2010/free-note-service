package com.freenote.app.handler;

import com.freenote.app.server.auth.impl.AcceptHandshakeImpl;
import com.freenote.app.server.http.HttpUpgradeRequest;
import com.freenote.app.server.http.HttpUpgradeResponse;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AcceptHandshakeHandlerTest {
    private final AcceptHandshakeImpl acceptHandshake = new AcceptHandshakeImpl();

    @Test
    void testHandle_WithValidSecWebSocketKey() throws Exception {
        var request = mock(HttpUpgradeRequest.class);
        when(request.getSecWebSocketKey()).thenReturn("test-key");
        when(request.getOrigin()).thenReturn("http://localhost:8082");
        when(request.getSecWebSocketExtensions()).thenReturn("permessage-deflate");
        when(request.getSecWebSocketVersion()).thenReturn("13");

        var expectedAccept = Base64.getEncoder().encodeToString(
                MessageDigest.getInstance("SHA-1")
                        .digest(("test-key" + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                                .getBytes(StandardCharsets.UTF_8))
        );

        var response = acceptHandshake.handle(request);

        assertEquals("101", response.getStatusCode());
        assertEquals("Switching Protocols", response.getStatusText());
        assertEquals("HTTP/1.1", response.getVersion());
        assertEquals("websocket", response.getUpgrade());
        assertEquals("Upgrade", response.getConnection());
        assertEquals(expectedAccept, response.getSecWebSocketAccept());
    }

    @Test
    void testHandle_WithNullSecWebSocketKey() {
        var request = mock(HttpUpgradeRequest.class);
        when(request.getSecWebSocketKey()).thenReturn(null);

        var response = acceptHandshake.handle(request);

        assertSame(HttpUpgradeResponse.EMPTY_UPGRADE_RESPONSE, response);
    }

    @Test
    void testHandle_WithEmptySecWebSocketKey() {
        var request = mock(HttpUpgradeRequest.class);
        when(request.getSecWebSocketKey()).thenReturn("");

        var response = acceptHandshake.handle(request);

        assertSame(HttpUpgradeResponse.EMPTY_UPGRADE_RESPONSE, response);
    }

    @Test
    void testHandle_WithException() {
        var request = mock(HttpUpgradeRequest.class);
        when(request.getSecWebSocketKey()).thenThrow(new RuntimeException("fail"));

        var response = acceptHandshake.handle(request);

        // Exception fallback -> should return a new empty HttpUpgradeResponse (not necessarily the EMPTY_UPGRADE_RESPONSE)
        assertNotNull(response);
        assertNull(response.getStatusCode());
    }
}
