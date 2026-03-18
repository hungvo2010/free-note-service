package com.freenote.app.server.core.v2;

import com.freenote.app.server.model.http.HttpUpgradeRequest;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

@AllArgsConstructor
@Log4j2
public class ProcessingState implements ConnectionState {
    private final HttpUpgradeRequest request;
    private final ByteBuffer byteBuffer;

    @Override
    public void handle(IncomingConnectionHandlerV2 handler, SocketChannel channel, SelectionKey key) throws IOException {
        log.info("Subsequent read from {}", channel.getRemoteAddress());
        handler.handleInComingMessage(channel, byteBuffer, request);
    }
}
