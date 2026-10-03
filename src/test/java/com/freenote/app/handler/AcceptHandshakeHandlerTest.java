package com.freenote.app.handler;

import com.freenote.app.server.handshaker.AcceptHandshakeHandler;
import com.freenote.app.server.handshaker.http.HttpUgrade;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AcceptHandshakeHandlerTest {
    private final AcceptHandshakeHandler.AcceptHandshakeImpl acceptHandshake = new AcceptHandshakeHandler.AcceptHandshakeImpl();

    @Test
    void testProcess_WithValidSecWebSocketKey() throws Exception {
        var request = mock(HttpUgrade.HttpUpgradeRequest.class);
        when(request.getSecWebSocketKey()).thenReturn("test-key");
        when(request.getOrigin()).thenReturn("http://localhost:8082");
        when(request.getSecWebSocketExtensions()).thenReturn("permessage-deflate");
        when(request.getSecWebSocketVersion()).thenReturn("13");

        var expectedAccept = Base64.getEncoder().encodeToString(
                MessageDigest.getInstance("SHA-1")
                        .digest(("test-key" + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                                .getBytes(StandardCharsets.UTF_8))
        );

        var response = acceptHandshake.process(request);

        assertEquals("101", response.getStatusCode());
        assertEquals("Switching Protocols", response.getStatusText());
        assertEquals("HTTP/1.1", response.getVersion());
        assertEquals("websocket", response.getUpgrade());
        assertEquals("Upgrade", response.getConnection());
        assertEquals(expectedAccept, response.getSecWebSocketAccept());
    }

    @Test
    void testProcess_WithNullSecWebSocketKey() {
        var request = mock(HttpUgrade.HttpUpgradeRequest.class);
        when(request.getSecWebSocketKey()).thenReturn(null);
        when(request.getSecWebSocketExtensions()).thenReturn(null);
        when(request.getSecWebSocketVersion()).thenReturn(null);

        var response = acceptHandshake.process(request);

        assertSame(HttpUgrade.HttpUpgradeResponse.EMPTY_UPGRADE_RESPONSE, response);
    }

    @Test
    void testProcess_MissingWebSocketKey_ShouldReturnEmptyResponse() {
        // Arrange
        HttpUgrade.HttpUpgradeRequest request = mock(HttpUgrade.HttpUpgradeRequest.class);
        when(request.getOrigin()).thenReturn("http://localhost:3000"); // Allowed origin
        when(request.getSecWebSocketKey()).thenReturn(null);           // Missing key
        when(request.getSecWebSocketExtensions()).thenReturn("ext");
        when(request.getSecWebSocketVersion()).thenReturn("13");

        // Act
        HttpUgrade.HttpUpgradeResponse response = acceptHandshake.process(request);

        // Assert
        assertSame(HttpUgrade.HttpUpgradeResponse.EMPTY_UPGRADE_RESPONSE, response);
    }

    @Test
    void testProcess_MissingWebSocketVersion_ShouldReturnEmptyResponse() {
        // Arrange
        HttpUgrade.HttpUpgradeRequest request = mock(HttpUgrade.HttpUpgradeRequest.class);
        when(request.getOrigin()).thenReturn("http://localhost:3000"); // Valid origin
        when(request.getSecWebSocketKey()).thenReturn("key");
        when(request.getSecWebSocketExtensions()).thenReturn("ext");
        when(request.getSecWebSocketVersion()).thenReturn(null);       // Missing version

        // Act
        HttpUgrade.HttpUpgradeResponse response = acceptHandshake.process(request);

        // Assert
        assertSame(HttpUgrade.HttpUpgradeResponse.EMPTY_UPGRADE_RESPONSE, response);
    }

    @Test
    void testProcess_WithEmptySecWebSocketKey() {
        var request = mock(HttpUgrade.HttpUpgradeRequest.class);
        when(request.getSecWebSocketKey()).thenReturn("");

        var response = acceptHandshake.process(request);

        assertSame(HttpUgrade.HttpUpgradeResponse.EMPTY_UPGRADE_RESPONSE, response);
    }

    @Test
    void testProcess_WithException() {
        var request = mock(HttpUgrade.HttpUpgradeRequest.class);
        when(request.getSecWebSocketKey()).thenThrow(new RuntimeException("fail"));
        when(request.getOrigin()).thenReturn("http://localhost:8082");
        when(request.getSecWebSocketExtensions()).thenReturn("permessage-deflate");
        when(request.getSecWebSocketVersion()).thenReturn("13");

        var response = acceptHandshake.process(request);

        // Exception fallback -> should return a new empty HttpUpgradeResponse (not necessarily the EMPTY_UPGRADE_RESPONSE)
        assertNotNull(response);
        assertNull(response.getStatusCode());
    }
}
