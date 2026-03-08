package com.freenote.app.server.handler.impl;

import com.freenote.app.server.core.WebSocketConnection;
import com.freenote.app.server.frames.factory.FrameFactory;
import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.util.IOUtils;
import com.freenote.app.server.util.JSONUtils;
import lombok.extern.log4j.Log4j2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

@Log4j2
public class NIOCommonEndpointHandlerImpl extends CommonEndpointHandlerImpl {

    @Override
    public byte[] getRawBytes(InputWrapper inputWrapper) {
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

    @Override
    protected void sendResponse(WebSocketConnection webSocketConnection) throws IOException {
        var socketChannel = webSocketConnection.getSocketChannel();
        if (socketChannel == null) {
            super.sendResponse(webSocketConnection);
            return;
        }

        byte[] dataToWrite = null;
        if (!Objects.isNull(webSocketConnection.getResponseFrame())) {
            try (var baos = new ByteArrayOutputStream()) {
                IOUtils.writeOutPut(baos, webSocketConnection.getResponseFrame());
                dataToWrite = baos.toByteArray();
            }
        } else if (!Objects.isNull(webSocketConnection.getResponseObject())) {
            try (var baos = new ByteArrayOutputStream()) {
                IOUtils.writeOutPut(
                        baos,
                        FrameFactory.SERVER.createTextFrame(
                                JSONUtils.toJSONString(webSocketConnection.getResponseObject().getResponseData()
                                )));
                dataToWrite = baos.toByteArray();
            }
        }

        if (dataToWrite != null) {
            ByteBuffer buffer = ByteBuffer.wrap(dataToWrite);
            while (buffer.hasRemaining()) {
                socketChannel.write(buffer);
            }
        }
    }
}
