package com.freenote.app.server.core;

import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.model.OutputWrapper;
import com.freenote.app.server.model.ws.CommonRequestObject;
import com.freenote.app.server.model.ws.CommonResponseObject;
import lombok.Builder;
import lombok.Data;

import java.io.InputStream;

@Data
@Builder
public class WebSocketConnection {
    private InputStream inputStream;
    private OutputWrapper outputWrapper;
    private CommonRequestObject requestObject;
    private CommonResponseObject responseObject;
    private WebSocketFrame requestFrame;
    private WebSocketFrame responseFrame;
}
