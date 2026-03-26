package com.freenote.app.server.core.v2;

import java.io.IOException;

public interface ConnectionState {
    void handle(ModernIncomingConnectionHandler handler, ReadableContext context) throws IOException;
}
