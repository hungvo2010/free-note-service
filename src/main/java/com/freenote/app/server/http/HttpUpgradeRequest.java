package com.freenote.app.server.http;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.net.URI;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HttpUpgradeRequest {
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
    private String webSocketProtocol;

    public static class HttpUpgradeRequestBuilder {
        public HttpUpgradeRequestBuilder uri(String uri) {
            this.uri = uri;
            this.path = URI.create(uri).getPath();
            return this;
        }
    }
}
