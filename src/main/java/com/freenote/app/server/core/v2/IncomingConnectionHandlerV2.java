package com.freenote.app.server.core.v2;

import com.freenote.app.server.model.http.HttpUpgradeRequest;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface IncomingConnectionHandlerV2 {
    void handleInComingMessage(ReadableContext socketChannel, ByteBuffer byteBuffer, HttpUpgradeRequest upgradeRequest) throws IOException;
    HttpUpgradeRequest handShake(ReadableContext socketChannel, ByteBuffer byteBuffer) throws IOException;
}
