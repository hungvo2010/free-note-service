package com.freenote.app.server.core.startup;

import com.freenote.app.server.core.config.ServerSocketConfig;
import com.freenote.app.server.core.connection.PerConnectionHandler;

public interface ServerBootstrap {
    void start(PerConnectionHandler handler, ServerSocketConfig config);
}
