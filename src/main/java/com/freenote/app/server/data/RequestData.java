package com.freenote.app.server.data;

import lombok.Data;

import java.util.UUID;

@Data
public class RequestData {
    private String requestId;
    private String traceId;
    private long timestamp;

    public RequestData() {
        requestId = UUID.randomUUID().toString();
        traceId = UUID.randomUUID().toString();
        timestamp = System.currentTimeMillis();
    }
}
