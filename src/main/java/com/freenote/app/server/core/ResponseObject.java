package com.freenote.app.server.core;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResponseObject<T extends ResponseData> {
    private int type;
    private T responseData;

    public T getResponseData(Class<T> clazz) {
        return clazz.cast(responseData);
    }
}
