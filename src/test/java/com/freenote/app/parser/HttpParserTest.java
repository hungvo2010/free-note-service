package com.freenote.app.parser;

import com.freenote.app.server.parser.HttpParser;
import com.freenote.app.server.parser.impl.HttpParserImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

class HttpParserTest {
    private final HttpParser httpParser = new HttpParserImpl();

    @Test
    void givenRawUpgradeRequest_whenParse_thenOk() throws IOException {
        InputStream targetStream = getInputStream();
        var request = httpParser.parse(targetStream);
        Assertions.assertEquals("HTTP/1.1", request.getVersion());
        Assertions.assertEquals("GET", request.getMethod());
        Assertions.assertEquals("ws://localhost:8189/example", request.getUri());
        Assertions.assertEquals("/example", request.getPath());
        Assertions.assertEquals("localhost:8189", request.getHost());
        Assertions.assertEquals("Upgrade", request.getConnection());
        Assertions.assertEquals("websocket", request.getUpgrade());
        Assertions.assertEquals("null", request.getOrigin());
        Assertions.assertEquals("13", request.getSecWebSocketVersion());
        Assertions.assertEquals("TixQkgsxKyup9IZVxSoe1w==", request.getSecWebSocketKey());
    }

    private static InputStream getInputStream() {
        String initialString = """
                GET ws://localhost:8189/example HTTP/1.1
                Host: localhost:8189
                Connection: Upgrade
                Pragma: no-cache
                Cache-Control: no-cache
                User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36
                Upgrade: websocket
                Origin: null
                Sec-WebSocket-Version: 13
                Accept-Encoding: gzip, deflate, br, zstd
                Accept-Language: en-US,en;q=0.9,vi-VN;q=0.8,vi;q=0.7
                Sec-WebSocket-Key: TixQkgsxKyup9IZVxSoe1w==
                Sec-WebSocket-Extensions: permessage-deflate; client_max_window_bits
                """;
        return new ByteArrayInputStream(initialString.getBytes());
    }

    @Test
    void givenInvalidHttpRequest_whenParse_thenOk() throws IOException {
        String initialString = "";
        InputStream targetStream = new ByteArrayInputStream(initialString.getBytes());
        var request = httpParser.parse(targetStream);
        Assertions.assertNull(request.getMethod());
        Assertions.assertNull(request.getOrigin());
    }
}
