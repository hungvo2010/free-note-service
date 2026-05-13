package com.freenote.app.server.handler.endpoint;

import com.freenote.app.server.core.connection.WebSocketConnection;
import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.parser.impl.ByteBufferFrameParser;
import com.freenote.app.server.util.IOUtils;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

@Log4j2
public class NIOCommonEndpoint extends AbstractEndpointHandler {

    public NIOCommonEndpoint() {
        super(new ByteBufferFrameParser());
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
