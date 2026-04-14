package com.freenote.app.server.parser.impl;

import com.freenote.app.server.frames.factory.FrameFactory;
import com.freenote.app.server.messages.ws.WebSocketFrame;
import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.parser.WebSocketFrameParser;
import com.freenote.app.server.util.IOUtils;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.InputStream;

@Log4j2
public class ByteBufferFrameParser implements WebSocketFrameParser {

    @Override
    public WebSocketFrame parseFrame(InputWrapper inputWrapper) throws IOException {
        byte[] rawBytes = getRawBytes(inputWrapper);
        if (rawBytes.length == 0) {
            throw new IOException("Failed to read WebSocket frame from ByteBuffer: 0 bytes read");
        }
        return FrameFactory.CLIENT.createFrameFromBytes(rawBytes);
    }

    private byte[] getRawBytes(InputWrapper inputWrapper) {
        var byteBuffer = inputWrapper.getChannelBuffer();
        if (byteBuffer.position() > 0) {
            byteBuffer.flip();
        }
        log.info("Reading WebSocket frame from ByteBuffer (limit: {}, remaining: {})", byteBuffer.limit(), byteBuffer.remaining());
        try (InputStream inputStream = IOUtils.newInputStream(byteBuffer)) {
            return IOUtils.getRawBytes(inputStream);
        } catch (IOException e) {
            log.error("Failed to parse WebSocket frame from ByteBuffer", e);
            return new byte[0];
        }
    }
}
