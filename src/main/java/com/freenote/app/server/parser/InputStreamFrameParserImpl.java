package com.freenote.app.server.parser;

import com.freenote.app.server.frames.ws.WebSocketFrame;
import com.freenote.app.server.model.ws.NetworkRequestData;

import java.io.IOException;

public class InputStreamFrameParserImpl implements WebSocketFrameParser {
    private final FullFrameParser fullFrameParser = new FullFrameParser();

    @Override
    public WebSocketFrame parseFrame(NetworkRequestData networkRequest) throws IOException {
        return fullFrameParser.parseFrame(networkRequest);
    }
}
