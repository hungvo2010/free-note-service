package com.freenote.app.server.core;

import java.util.UUID;

public class RequestData {
    private String requestId;
    private String traceId;

    public RequestData() {
        requestId = UUID.randomUUID().toString();
        traceId = UUID.randomUUID().toString();
    }
}
