package com.freenote.app.server.core.v2;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public interface IncomingConnectionHandlerV2 {
    void handle(SocketChannel socketChannel) throws IOException;
}
