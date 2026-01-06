package com.freenote.app.server.core.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Setter;

import java.net.Socket;

@AllArgsConstructor
@Builder
@Setter
public class RequestObject<T extends RequestData> {
    private int requestType;
    private Socket socket;
    private String origin;
    private T requestData;

    private T getRequestData(Class<T> clazz) {
        return (clazz.cast(requestData));
    }
}
