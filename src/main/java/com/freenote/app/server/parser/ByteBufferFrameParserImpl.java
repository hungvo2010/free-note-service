package com.freenote.app.server.parser;

import com.freenote.app.server.frames.factory.FrameFactory;
import com.freenote.app.server.frames.ws.WebSocketFrame;
import com.freenote.app.server.model.ws.NetworkRequestData;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

@Log4j2
public class ByteBufferFrameParserImpl implements WebSocketFrameParser {

    @Override
    public WebSocketFrame parseFrame(NetworkRequestData networkRequest) throws IOException {
        byte[] rawBytes = getRawBytes(networkRequest);
        if (rawBytes.length == 0) {
            throw new IOException("Failed to read WebSocket frame from ByteBuffer: 0 bytes read");
        }
        return FrameFactory.CLIENT.createFrameFromBytes(rawBytes);
    }

    private byte[] getRawBytes(NetworkRequestData networkRequest) throws IOException {
        var nioRequest = networkRequest;
        nioRequest.prepareForRead();
        return nioRequest.readFrameBytes();
    }
}
