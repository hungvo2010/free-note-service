package com.freenote.app.server.handler;

import com.freenote.app.server.frames.base.WebSocketFrame;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface URIHandler {
    boolean handle(InputStream inputStream, OutputStream outputStream); // todo: check why boolean return type

    boolean continuationHandler(List<WebSocketFrame> clientFrame, InputStream inputStream, OutputStream outputStream) throws IOException;
}
