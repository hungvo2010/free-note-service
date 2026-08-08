package com.freenote.app.server.model.ws;

import com.freenote.app.server.model.TraceResponseData;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CommonResponseObject<T extends TraceResponseData> {
    private T responseData;

    public T getResponseData(Class<T> clazz) {
        return clazz.cast(responseData);
    }
}
