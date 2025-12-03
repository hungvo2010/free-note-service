package com.freenote.app.server.handler;

import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.model.InputWrapper;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public interface URIHandler {
    boolean handle(InputWrapper inputWrapper, OutputStream outputStream) throws IOException; // TODO: check why boolean return type

    boolean continuationHandler(List<WebSocketFrame> clientFrame, InputWrapper inputWrapper, OutputStream outputStream) throws IOException;
}