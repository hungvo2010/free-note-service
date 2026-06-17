package com.freenote.app.server.parser;

import com.freenote.app.server.frames.factory.FrameFactory;
import com.freenote.app.server.frames.ws.WebSocketFrame;
import com.freenote.app.server.model.ws.NetworkRequestData;
import com.freenote.app.server.util.IOUtils;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.InputStream;

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
        var byteBuffer = networkRequest.getChannelBuffer();
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
