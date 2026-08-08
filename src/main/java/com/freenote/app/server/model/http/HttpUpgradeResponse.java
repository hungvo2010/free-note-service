package com.freenote.app.server.model.http;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class HttpUpgradeResponse {
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
}
