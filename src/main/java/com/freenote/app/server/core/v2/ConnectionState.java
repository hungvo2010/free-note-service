package com.freenote.app.server.core.v2;

import java.io.IOException;

public interface ConnectionState {
    void handle(IncomingConnectionHandlerV2 handler, ReadableContext context) throws IOException;
}
