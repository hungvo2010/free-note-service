package com.freenote.app.server.model.ws;

import com.freenote.app.server.model.TraceRequestData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Builder
@Setter
@Getter
public class AppRequestData<T extends TraceRequestData> {
    private String requestOrigin;
    private T requestData;

    private T getRequestData(Class<T> clazz) {
        return (clazz.cast(requestData));
    }
}
