package com.freenote.app.server.parser;

import com.freenote.app.server.messages.ws.WebSocketFrame;
import com.freenote.app.server.model.InputWrapper;

import java.io.IOException;

public interface WebSocketFrameParser {
    WebSocketFrame parseFrame(InputWrapper inputWrapper) throws IOException;
}