package com.freenote.app.http;

import com.freenote.app.server.model.http.HttpUpgradeResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HttpResponseTest {
    @Test
    void givenSpecialCaseThenSuccess() {
        assertNull(HttpUpgradeResponse.EMPTY_UPGRADE_RESPONSE.getUpgrade());
    }

    @Test
    void givenHttpResponseThenToStringSuccess() {
        var stringVersion = HttpUpgradeResponse.builder()
                .statusCode("101")
                .statusText("Switching Protocols")
                .version("HTTP/1.1")
                .upgrade("websocket")
                .connection("Upgrade")
                .secWebSocketAccept("0PKbUcsc7r8Fb6wl29FDzcZfB4g=")
                .build()
                .toString();
        assertEquals("HTTP/1.1 101 Switching Protocols\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Accept: 0PKbUcsc7r8Fb6wl29FDzcZfB4g=\r\n\r\n", stringVersion);
    }
}
