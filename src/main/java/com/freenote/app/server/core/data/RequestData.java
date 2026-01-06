package com.freenote.app.server.core.data;

import lombok.Data;

import java.util.UUID;

@Data
public class RequestData {
    private String requestId;
    private String traceId;

    public RequestData() {
        requestId = UUID.randomUUID().toString();
        traceId = UUID.randomUUID().toString();
    }
}
