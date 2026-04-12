package com.freenote.app.server.core.connection.state;

import com.freenote.app.server.core.nio.ModernIncomingConnectionHandler;
import com.freenote.app.server.core.context.ReadableContext;

import java.io.IOException;

public interface ConnectionState {
    void handle(ModernIncomingConnectionHandler handler, ReadableContext context) throws IOException;
}
