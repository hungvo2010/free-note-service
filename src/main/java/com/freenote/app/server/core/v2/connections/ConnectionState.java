package com.freenote.app.server.core.v2.connections;

import com.freenote.app.server.core.v2.ModernIncomingConnectionHandler;
import com.freenote.app.server.core.v2.context.ReadableContext;

import java.io.IOException;

public interface ConnectionState {
    void handle(ModernIncomingConnectionHandler handler, ReadableContext context) throws IOException;
}
