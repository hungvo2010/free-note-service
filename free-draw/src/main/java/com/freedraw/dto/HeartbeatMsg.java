package com.freedraw.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.freenote.app.server.model.TraceResponseData;
import com.freenote.app.server.model.enums.MsgType;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HeartbeatMsg  extends TraceResponseData {
    private MsgType msgType;
    private String message;
    private long pingAt;
    private long receivedPingAt;
    private long pongAt;
}
