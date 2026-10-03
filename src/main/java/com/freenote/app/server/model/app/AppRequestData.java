package com.freenote.app.server.model.app;

import lombok.*;

import java.util.UUID;

@AllArgsConstructor
@Builder
@Getter
@NoArgsConstructor
@Setter
public class AppRequestData {
    private String requestOrigin;

    @Data
    public static class TraceRequestData<T extends AppRequestData> {
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
}
