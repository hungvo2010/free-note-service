package com.freenote.app.server.model.ws;

import lombok.Data;

import java.util.UUID;

@Data
public class TraceRequestData<T extends AppRequestData> {
    private String requestId;
    private String traceId;
    private long timestamp;
    private T requestData;

    public TraceRequestData() {
        requestId = UUID.randomUUID().toString();
        traceId = UUID.randomUUID().toString();
        timestamp = System.currentTimeMillis();
    }

    private T getRequestData(Class<T> clazz) {
        return (clazz.cast(requestData));
    }
}
