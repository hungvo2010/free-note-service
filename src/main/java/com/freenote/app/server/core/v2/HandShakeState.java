package com.freenote.app.server.core.v2;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

@AllArgsConstructor
@Getter
@NoArgsConstructor
@Setter
@Log4j2
public class HandShakeState implements ConnectionState {
    private ByteBuffer byteBuffer = ByteBuffer.allocateDirect(2048);

    @Override
    public void handle(IncomingConnectionHandlerV2 handler, SocketChannel channel, SelectionKey key) throws IOException {
        try {
            log.info("Performing handshake for {}", channel.getRemoteAddress());
            var upgradeRequest = handler.handShake(channel, byteBuffer);
            if (upgradeRequest != null) {
                key.attach(new ProcessingState(upgradeRequest, byteBuffer));
            }
        } catch (Exception e) {
            log.error("Handshake failed: ", e);
            channel.close();
        }
    }
}