package com.freenote.app.server.dto;


import com.freenote.app.server.model.TraceResponseData;
import com.freenote.app.server.model.enums.MsgType;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Getter
@Data
@Builder
public class HeartbeatMsg  extends TraceResponseData {
    private MsgType msgType;
    private String message;
    private long pingAt;
    private long receivedPingAt;
    private long pongAt;
}
