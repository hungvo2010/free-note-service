package com.freenote.app.server.core.v2;

import com.freenote.app.server.model.http.HttpUpgradeRequest;
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
public class HandshakeState implements ConnectionState {
    private ByteBuffer byteBuffer = ByteBuffer.allocateDirect(2048);

    @Override
    public void handle(IncomingConnectionHandlerV2 handler, SocketChannel channel, SelectionKey key) throws IOException {
        log.info("Performing handshake for {}", channel.getRemoteAddress());
        var upgradeRequest = handler.handShake(channel, byteBuffer);
        // Sau khi xong, tự nâng cấp trạng thái của chính mình trên SelectionKey (Context)
        if (upgradeRequest != null) {
            key.attach(new ProcessingState(upgradeRequest, byteBuffer));
        }
    }
}
