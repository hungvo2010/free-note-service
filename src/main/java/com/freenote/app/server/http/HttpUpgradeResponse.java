package com.freenote.app.server.http;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
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
    public static final HttpUpgradeResponse EMPTY_UPGRADE_RESPONSE = new HttpUpgradeResponse();

    @Override
    public String toString() {
        String statusLine = String.join(" ", version, statusCode, statusText);
        return String.join("\n",
                statusLine,
                "Upgrade: " + upgrade,
                "Connection: " + connection,
                "Sec-WebSocket-Accept: " + secWebSocketAccept,
                "", "" // required to terminate HTTP headers
        );
    }
}
