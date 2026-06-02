package com.freenote.app.server.core.nio;

import com.freenote.app.server.core.context.ReadableContext;
import com.freenote.app.server.model.http.HttpUpgradeRequest;

import java.io.IOException;

public interface ModernIncomingConnectionHandler {
    void handleInComingMessage(ReadableContext socketChannel, HttpUpgradeRequest upgradeRequest) throws IOException;
    HttpUpgradeRequest handShake(ReadableContext socketChannel) throws IOException;
}
