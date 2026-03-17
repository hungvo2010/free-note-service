package com.freenote.app.server.core.v2;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public interface ConnectionState {
    void handle(IncomingConnectionHandlerV2 handler, SocketChannel channel, SelectionKey key) throws IOException;
}
