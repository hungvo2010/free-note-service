package com.freenote.app.server.model;

import lombok.Data;

import java.util.UUID;

@Data
public class TraceRequestData {
    private String requestId;
    private String traceId;
    private long timestamp;

    public TraceRequestData() {
        requestId = UUID.randomUUID().toString();
        traceId = UUID.randomUUID().toString();
        timestamp = System.currentTimeMillis();
    }
}
