package com.freenote.app.server.parser.impl;

import com.freenote.app.server.http.HttpUpgradeRequest;
import com.freenote.app.server.parser.HttpParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class HttpParserImpl implements HttpParser {
    private static final Logger log = LogManager.getLogger(HttpParserImpl.class);

    public HttpUpgradeRequest parse(InputStream inputStream) {
        try {
            var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            var requestBuilder = HttpUpgradeRequest.builder();

            // First line: GET /path HTTP/1.1
            var requestLine = reader.readLine();
            if (requestLine == null) throw new IOException("Empty request");
            var requestEndpoint = requestLine.split(" ");
            requestBuilder.method(requestEndpoint[0]);
            requestBuilder.uri(requestEndpoint[1]);
            requestBuilder.version(requestEndpoint[2]);

            var headers = new HashMap<String, String>();
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                int colonIndex = line.indexOf(":");
                var name = line.substring(0, colonIndex).trim();
                var value = line.substring(colonIndex + 1).trim();
                headers.put(name, value);
            }

            requestBuilder.secWebSocketKey(headers.get("Sec-WebSocket-Key"));
            requestBuilder.secWebSocketVersion(headers.get("Sec-WebSocket-Version"));
            requestBuilder.secWebSocketExtensions(headers.get("Sec-WebSocket-Extensions"));
            requestBuilder.upgrade(headers.get("Upgrade"));
            requestBuilder.connection(headers.get("Connection"));
            requestBuilder.host(headers.get("Host"));
            requestBuilder.origin(headers.get("Origin"));

            return requestBuilder.build();
        } catch (Exception e) {
            log.error("Failed to parse WebSocket upgrade request", e);
            return new HttpUpgradeRequest();
        }
    }
}
