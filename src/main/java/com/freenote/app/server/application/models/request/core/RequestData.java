package com.freenote.app.server.application.models.request.core;

import java.util.UUID;

public class RequestData {
    private String requestId;
    private String traceId;

    public RequestData() {
        requestId = UUID.randomUUID().toString();
        traceId = UUID.randomUUID().toString();
    }
}
