package com.freenote.app.server.core.connection;

import com.freenote.app.server.core.context.ConnectionContext;
import com.freenote.app.server.exceptions.ConnectionException;

 import java.io.IOException;

public interface PerConnectionHandler {
    void handle(ConnectionContext context) throws ConnectionException, IOException;
}
