package com.freenote.app.server.parser;

import com.freenote.app.server.frames.ws.WebSocketFrame;
import com.freenote.app.server.model.ws.NetworkRequestData;

import java.io.IOException;

public interface WebSocketFrameParser {
    WebSocketFrame parseFrame(NetworkRequestData networkRequest) throws IOException;
}