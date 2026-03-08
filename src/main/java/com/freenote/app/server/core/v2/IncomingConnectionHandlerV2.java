package com.freenote.app.server.core.v2;

import com.freenote.app.server.http.HttpUpgradeRequest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public interface IncomingConnectionHandlerV2 {
    void handle(SocketChannel socketChannel, ByteBuffer byteBuffer, HttpUpgradeRequest upgradeRequest) throws IOException;
    HttpUpgradeRequest handShake(SocketChannel socketChannel, ByteBuffer byteBuffer) throws IOException;
}
