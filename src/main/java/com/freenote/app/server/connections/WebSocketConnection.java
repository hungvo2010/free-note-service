package com.freenote.app.server.connections;

import com.freenote.app.server.core.RequestObject;
import com.freenote.app.server.core.ResponseObject;
import com.freenote.app.server.frames.base.WebSocketFrame;
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
    private WebSocketFrame requestFrame;
    private WebSocketFrame responseFrame;
}
