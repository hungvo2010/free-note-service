package com.freenote.app.server.http;

import lombok.Builder;

@Builder
public class HttpUpgradeResponse {
    //    HTTP/1.1 101 Switching Protocols
//    Upgrade: websocket
//    Connection: Upgrade
//    Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
    private String version;
    private String statusCode;
    private String statusText;
    private String upgrade;
    private String connection;
    private String secWebSocketAccept;

    @Override
    public String toString() {
        String endpoint = String.join(" ", version, statusCode, statusText);
        String upgrade = "Upgrade: " + this.upgrade;
        String connection = "Connection: " + this.upgrade;
        String secWebSocketAccept = "Sec-WebSocket-Accept: " + this.secWebSocketAccept;
        return String.join("\n", endpoint, upgrade, connection, secWebSocketAccept);
    }
}
