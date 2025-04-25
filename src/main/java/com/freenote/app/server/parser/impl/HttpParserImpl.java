package com.freenote.app.server.parser.impl;

import com.freenote.app.server.http.HttpUpgradeRequest;
import com.freenote.app.server.http.exceptions.UpgradeParserException;
import com.freenote.app.server.parser.HttpParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class HttpParserImpl implements HttpParser {
    public HttpUpgradeRequest parse(InputStream inputStream) {
        try (var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            var requestBuilder = HttpUpgradeRequest.builder();
            var requestEndpoint = reader.readLine().split(" ");
            requestBuilder.method(requestEndpoint[0]);
            requestBuilder.uri(requestEndpoint[1]);
            requestBuilder.version(requestEndpoint[2]);
            var headers = new HashMap<String, String>();
            while (reader.ready()) {
                var headerPair = reader.readLine().split(": ");
                headers.put(headerPair[0], headerPair[1]);
            }
            requestBuilder.secWebSocketKey(headers.get("Sec-WebSocket-Key"));
            requestBuilder.secWebSocketVersion(headers.get("Sec-WebSocket-Version"));
            requestBuilder.upgrade(headers.get("Upgrade"));
            requestBuilder.connection(headers.get("Connection"));
            requestBuilder.host(headers.get("Host"));
            requestBuilder.host(headers.get("Origin"));
            return requestBuilder.build();
        } catch (IOException e) {
            throw new UpgradeParserException(e);
        }
    }
}
