package com.freedraw.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.freenote.app.server.model.app.AppResponseData;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HeartbeatMsg  extends AppResponseData.TraceResponseData {
    private MsgType msgType;
    private String message;
    private long pingAt;
    private long receivedPingAt;
    private long pongAt;

    public enum MsgType {
        PING,
        PONG
    }
}
