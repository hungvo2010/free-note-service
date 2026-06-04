package com.freenote.app.server.parser;

import com.freenote.app.server.messages.ws.WebSocketFrame;
import com.freenote.app.server.model.InputWrapper;

import java.io.IOException;

public class InputStreamFrameParserImpl implements WebSocketFrameParser {
    private final FullFrameParser fullFrameParser = new FullFrameParser();

    @Override
    public WebSocketFrame parseFrame(InputWrapper inputWrapper) throws IOException {
        return fullFrameParser.parseFrame(inputWrapper);
    }
}
