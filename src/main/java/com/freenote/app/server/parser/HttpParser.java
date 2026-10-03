package com.freenote.app.server.parser;

import com.freenote.app.server.handshaker.http.HttpUgrade;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public interface HttpParser {
    HttpUgrade.HttpUpgradeRequest parse(InputStream inputStream) throws IOException;

    HttpUgrade.HttpUpgradeRequest parse(byte[] data) throws IOException;

    class HttpParserImpl implements HttpParser {
        private static final Logger log = LogManager.getLogger(HttpParserImpl.class);

        public HttpUgrade.HttpUpgradeRequest parse(InputStream inputStream) {
            try {
                var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                var endpointHeaders = parseRequestEndpoint(reader);
                var paramHeaders = parseHeaders(reader);
                paramHeaders.putAll(endpointHeaders);

                return setHeadersToRequest(paramHeaders);
            } catch (Exception e) {
                log.error("Failed to parse WebSocket upgrade request", e);
                return new HttpUgrade.HttpUpgradeRequest();
            }

        }

        @Override
        public HttpUgrade.HttpUpgradeRequest parse(byte[] data) throws IOException {
            if (data == null || data.length == 0) {
                return new HttpUgrade.HttpUpgradeRequest();
            }
            try (InputStream inputStream = new ByteArrayInputStream(data)) {
                return parse(inputStream);
            }
        }

        private Map<String, String> parseRequestEndpoint(BufferedReader reader) throws IOException {
            // First line: GET /path HTTP/1.1
            var requestLine = reader.readLine();
            if (requestLine == null) throw new IOException("Empty request");
            var requestEndpoint = requestLine.split(" ");

            return Map.of(
                    "method", requestEndpoint[0],
                    "uri", requestEndpoint[1],
                    "version", requestEndpoint[2]
            );
        }

        private Map<String, String> parseHeaders(BufferedReader reader) throws IOException {
            var headers = new HashMap<String, String>();
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                log.info("Header: {}", line);
                int colonIndex = line.indexOf(":");
                var name = line.substring(0, colonIndex).trim();
                var value = line.substring(colonIndex + 1).trim();
                headers.put(name, value);
            }

            return headers;
        }

        private HttpUgrade.HttpUpgradeRequest setHeadersToRequest(Map<String, String> headers) {
            var requestBuilder = HttpUgrade.HttpUpgradeRequest.builder();
            requestBuilder.secWebSocketKey(headers.get("Sec-WebSocket-Key"));
            requestBuilder.secWebSocketVersion(headers.get("Sec-WebSocket-Version"));
            requestBuilder.secWebSocketExtensions(headers.get("Sec-WebSocket-Extensions"));
            requestBuilder.upgrade(headers.get("Upgrade"));
            requestBuilder.connection(headers.get("Connection"));
            requestBuilder.host(headers.get("Host"));
            requestBuilder.origin(headers.get("Origin"));

            requestBuilder.method(headers.get("method"));
            requestBuilder.path(headers.get("uri"));
            requestBuilder.version(headers.get("version"));

            return requestBuilder.build();
        }
    }
}
