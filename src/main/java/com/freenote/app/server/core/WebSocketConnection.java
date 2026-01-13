package com.freenote.app.server.core;

import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.model.ws.CommonRequestObject;
import com.freenote.app.server.model.ws.CommonResponseObject;
import lombok.Builder;
import lombok.Data;

import java.io.InputStream;
import java.io.OutputStream;

@Data
@Builder
public class WebSocketConnection {
    private InputStream inputStream;
    private OutputStream outputStream;
    private CommonRequestObject requestObject;
    private CommonResponseObject responseObject;
    private WebSocketFrame requestFrame;
    private WebSocketFrame responseFrame;
}
