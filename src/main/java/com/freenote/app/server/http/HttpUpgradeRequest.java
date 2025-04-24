package com.freenote.app.server.http;

import lombok.AllArgsConstructor;
import lombok.Builder;

@AllArgsConstructor
@Builder
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
    private String version;
    private String protocol;
    private String host;
    private String connection;
    private String secWebSocketKey;
    private String secWebSocketVersion;
    private String secWebSocketProtocol;
    private String secWebSocketExtensions;
    private String origin;

}
