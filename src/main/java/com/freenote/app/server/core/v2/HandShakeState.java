package com.freenote.app.server.core.v2;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.ByteBuffer;

@AllArgsConstructor
@Getter
@NoArgsConstructor
@Setter
@Log4j2
public class HandShakeState implements ConnectionState {
    private ByteBuffer byteBuffer = ByteBuffer.allocateDirect(2048);

    @Override
    public void handle(IncomingConnectionHandlerV2 handler, ReadableContext context) throws IOException {
        try {
            var upgradeRequest = handler.handShake(context, byteBuffer);
            if (upgradeRequest != null) {
                context.setState(new ProcessingState(upgradeRequest, byteBuffer));
            }
        } catch (Exception e) {
            log.error("Handshake failed: ", e);
            context.closeChannel();
        }
    }
}