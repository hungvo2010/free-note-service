package com.freenote.app.server.data;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ResponseData {
    private String requestId;
    private String traceId;
    private long timestamp;

    public ResponseData() {
        requestId = UUID.randomUUID().toString();
        traceId = UUID.randomUUID().toString();
        timestamp = System.currentTimeMillis();
    }
}
