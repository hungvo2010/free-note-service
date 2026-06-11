package com.freenote.app.server.core.startup;

import com.freenote.app.server.core.config.ServerSocketConfig;
import com.freenote.app.server.core.connection.IncomingConnectionHandler;

public interface ServerBootstrap {
    void start(IncomingConnectionHandler handler, ServerSocketConfig config) throws Exception;
}
