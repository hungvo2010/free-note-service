package com.freenote.app.server.core.v2;

import com.freenote.app.server.exceptions.ClientDisconnectException;
import com.freenote.app.server.model.http.HttpUpgradeRequest;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.ByteBuffer;

@AllArgsConstructor
@Log4j2
public class ProcessingState implements ConnectionState {
    private final HttpUpgradeRequest request;
    private final ByteBuffer byteBuffer;

    @Override
    public void handle(ModernIncomingConnectionHandler handler, ReadableContext context) throws IOException {
        try {
            context.setByteBuffer(byteBuffer);
            handler.handleInComingMessage(context, request);
        } catch (ClientDisconnectException e) {
            context.closeChannel();
        } catch (IOException e) {
            context.closeChannel();
            log.warn("[ProcessingState] Exception in handling new messages: {}", e.getMessage());
        }
    }
}
