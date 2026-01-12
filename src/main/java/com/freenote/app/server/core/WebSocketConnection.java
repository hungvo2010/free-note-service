package com.freenote.app.server.core;

import com.freenote.app.server.model.ws.CommonRequestObject;
import com.freenote.app.server.model.ws.CommonResponseObject;
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
    private CommonRequestObject requestObject;
    private CommonResponseObject response;
    private WebSocketFrame requestFrame;
    private WebSocketFrame responseFrame;
}
