package com.freenote.app.server.core.startup;

import com.freenote.app.server.config.ServerSocketConfig;
import com.freenote.app.server.core.connection.PerClientConnectionHandler;

public interface ServerBootstrap {
    void start(PerClientConnectionHandler handler, ServerSocketConfig config);
}
