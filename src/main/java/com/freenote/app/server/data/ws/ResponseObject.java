package com.freenote.app.server.data.ws;

import com.freenote.app.server.data.ResponseData;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResponseObject<T extends ResponseData> {
    private int requestType;
    private T responseData;

    public T getResponseData(Class<T> clazz) {
        return clazz.cast(responseData);
    }
}
