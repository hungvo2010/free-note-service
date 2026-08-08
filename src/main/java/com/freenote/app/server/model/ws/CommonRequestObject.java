package com.freenote.app.server.model.ws;

import com.freenote.app.server.model.TraceRequestData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.net.Socket;

@AllArgsConstructor
@Builder
@Setter
@Getter
public class CommonRequestObject<T extends TraceRequestData> {
    private Socket socket;
    private String origin;
    private T requestData;

    private T getRequestData(Class<T> clazz) {
        return (clazz.cast(requestData));
    }
}
