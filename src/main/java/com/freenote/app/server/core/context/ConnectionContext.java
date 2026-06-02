package com.freenote.app.server.core.context;

import com.freenote.app.server.model.http.HttpUpgradeRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

@AllArgsConstructor
@Builder
@Getter
public class ConnectionContext {
    private final Socket socket;
    private final SocketChannel socketChannel;
    private final HttpUpgradeRequest upgradeRequest;
    private final ByteBuffer byteBuffer;
    private final ReadableContext readableContext;
}
