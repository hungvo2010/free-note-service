package com.freenote.app.server.handler.impl;

import com.freenote.app.server.core.WebSocketConnection;
import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.util.IOUtils;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

@Log4j2
public class NIOCommonEndpointFrameEndpointHandlerImpl extends AbstractEndpointFrameEndpointHandlerImpl {

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
        byte[] dataToWrite = getDataToWrite(webSocketConnection);

        writeToChannel(socketChannel, dataToWrite);
    }

    private void writeToChannel(SocketChannel socketChannel, byte[] dataToWrite) throws IOException {
        if (dataToWrite != null) {
            ByteBuffer buffer = ByteBuffer.wrap(dataToWrite);
            while (buffer.hasRemaining()) {
                socketChannel.write(buffer);
            }
        }
    }

    private byte[] getDataToWrite(WebSocketConnection webSocketConnection) throws IOException {
        return webSocketConnection.getPayloadBytes();
    }
}
