package com.freenote.app.server.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class TraceResponseData {
    private String requestId;
    private String traceId;
    private long timestamp;

    public TraceResponseData() {
        requestId = UUID.randomUUID().toString();
        traceId = UUID.randomUUID().toString();
        timestamp = System.currentTimeMillis();
    }
}
