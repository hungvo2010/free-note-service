package com.freenote.app.server.parser.impl;

import com.freenote.app.server.messages.ws.WebSocketFrame;
import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.parser.WebSocketFrameParser;
import com.freenote.app.server.util.FullFrameParser;

import java.io.IOException;

public class InputStreamFrameParser implements WebSocketFrameParser {
    private final FullFrameParser fullFrameParser = new FullFrameParser();

    @Override
    public WebSocketFrame parseFrame(InputWrapper inputWrapper) throws IOException {
        return fullFrameParser.parseFrame(inputWrapper);
    }
}
