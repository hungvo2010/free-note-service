package com.freenote.app.server.model.ws;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class TraceResponseData<T extends AppResponseData> {
    private T responseData;
    private String requestId;
    private String traceId;
    private long timestamp;

    public TraceResponseData() {
        requestId = UUID.randomUUID().toString();
        traceId = UUID.randomUUID().toString();
        timestamp = System.currentTimeMillis();
    }

    public T getResponseData(Class<T> clazz) {
        return clazz.cast(responseData);
    }
}
