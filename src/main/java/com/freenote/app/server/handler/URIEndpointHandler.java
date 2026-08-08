package com.freenote.app.server.handler;

import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.model.OutputWrapper;

import java.io.IOException;
import java.util.List;

public interface URIEndpointHandler {
    boolean handle(InputWrapper inputWrapper, OutputWrapper outputWrapper) throws IOException; // TODO: check why boolean return type

    boolean continuationHandler(List<WebSocketFrame> clientFrame, InputWrapper inputWrapper, OutputWrapper outputWrapper) throws IOException;
}