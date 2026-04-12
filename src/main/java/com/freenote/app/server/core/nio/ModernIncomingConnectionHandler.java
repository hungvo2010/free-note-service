package com.freenote.app.server.core.nio;

import com.freenote.app.server.core.context.ReadableContext;
import com.freenote.app.server.model.http.HttpUpgradeRequest;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface ModernIncomingConnectionHandler {
    void handleInComingMessage(ReadableContext socketChannel, HttpUpgradeRequest upgradeRequest) throws IOException;
    HttpUpgradeRequest handShake(ReadableContext socketChannel, ByteBuffer byteBuffer) throws IOException;
}
