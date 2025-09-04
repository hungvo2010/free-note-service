package com.freenote.app.server.handler;

import com.freenote.app.server.frames.base.WebSocketFrame;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface URIHandler {
    boolean handle(InputStream inputStream, OutputStream outputStream);

    boolean continuationHandler(WebSocketFrame clientFrame, InputStream inputStream, OutputStream outputStream) throws IOException;
}
