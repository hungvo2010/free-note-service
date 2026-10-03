package com.freenote.app.server.handshaker.http;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.nio.charset.StandardCharsets;

public class HttpUgrade {
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HttpUpgradeRequest {
        //    GET /chat HTTP/1.1
    //    Host: server.example.com
    //    Upgrade: websocket
    //    Connection: Upgrade
    //    Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
    //    Origin: http://example.com
    //    Sec-WebSocket-Protocol: chat, superchat
    //    Sec-WebSocket-Version: 13
        private String method;
        private String uri;
        private String path;
        private String version;
        private String host;
        private String upgrade;
        private String connection;
        private String secWebSocketKey;
        private String origin;
        private String secWebSocketVersion;
        private String secWebSocketExtensions;
        private String webSocketProtocol;
        public static final HttpUpgradeRequest EMPTY_UPGRADE_REQUEST = new HttpUpgradeRequest();

        public static class HttpUpgradeRequestBuilder {
            public HttpUpgradeRequestBuilder uri(String uri) {
                this.uri = uri;
                this.path = URI.create(uri).getPath();
                return this;
            }

            public HttpUpgradeRequest build() {
                if (secWebSocketKey == null || secWebSocketKey.isEmpty()
                        || secWebSocketVersion == null || secWebSocketVersion.isEmpty()) {
                    return EMPTY_UPGRADE_REQUEST;
                }
                return new HttpUpgradeRequest(
                        this.method,
                        this.uri,
                        this.path,
                        this.version,
                        this.host,
                        this.upgrade,
                        this.connection,
                        this.secWebSocketKey,
                        this.origin,
                        this.secWebSocketVersion,
                        this.secWebSocketExtensions,
                        this.webSocketProtocol
                );
            }
        }

        @Override
        public String toString() {
            return String.join("\r\n",
                    method + " " + path + " " + version,
                    "Host: " + host,
                    "Upgrade: " + upgrade,
                    "Connection: " + connection,
                    "Sec-WebSocket-Key: " + secWebSocketKey,
                    "Origin: " + origin,
                    "Sec-WebSocket-Version: " + secWebSocketVersion,
                    "Sec-WebSocket-Extensions: " + secWebSocketExtensions,
                    "Sec-WebSocket-Protocol: " + webSocketProtocol,
                    "", "" // required to terminate HTTP headers
            );
        }
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class HttpUpgradeResponse {
        //    HTTP/1.1 101 Switching Protocols
    //    Upgrade: websocket
    //    Connection: Upgrade
    //    Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
        private String version;
        @Builder.Default
        private String statusCode = "-1";
        private String statusText;
        private String upgrade;
        private String connection;
        private String secWebSocketAccept;
        private HttpUpgradeRequest httpUpgradeRequest;
        public static final HttpUpgradeResponse EMPTY_UPGRADE_RESPONSE = new HttpUpgradeResponse();

        @Override
        public String toString() {
            String statusLine = String.join(" ", version, statusCode, statusText);
            return String.join("\r\n",
                    statusLine,
                    "Upgrade: " + upgrade,
                    "Connection: " + connection,
                    "Sec-WebSocket-Accept: " + secWebSocketAccept,
                    "", "" // required to terminate HTTP headers
            );
        }

        public byte[] toRawBytes() {
            return this.toString().getBytes(StandardCharsets.UTF_8);
        }
    }
}
