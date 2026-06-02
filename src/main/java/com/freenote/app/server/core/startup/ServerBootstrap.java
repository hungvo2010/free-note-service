package com.freenote.app.server.core.startup;

import com.freenote.app.server.core.config.ServerSocketConfig;
import com.freenote.app.server.core.connection.IncomingConnectionHandler;
import com.freenote.app.server.socket.ServerSocketFactory;

public interface ServerBootstrap {
    static ServerBootstrap from(ServerSocketFactory socketFactory) {
        return null;
    }

    void start(IncomingConnectionHandler handler, ServerSocketConfig config) throws Exception;
}
