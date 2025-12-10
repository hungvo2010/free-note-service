package com.freenote.app.server.connections;

import com.freenote.app.server.application.models.request.core.RequestObject;
import com.freenote.app.server.application.models.request.core.ResponseObject;
import lombok.Builder;
import lombok.Data;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

@Data
@Builder
public class WebSocketConnection {
    private InputStream inputStream;
    private OutputStream outputStream;
    private Socket socket;
    private RequestObject requestObject;
    private ResponseObject response;
}
