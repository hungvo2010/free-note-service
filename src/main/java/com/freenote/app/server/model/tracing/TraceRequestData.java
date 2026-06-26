package com.freenote.app.server.model.tracing;

import com.freenote.app.server.model.app.AppRequestData;
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
