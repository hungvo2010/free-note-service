package com.freenote.app.server.core.startup;

import com.freenote.app.server.core.config.ServerSocketConfig;
import com.freenote.app.server.core.connection.IncomingConnectionHandler;
import com.freenote.app.server.core.legacy.startup.LegacyBootstrap;
import com.freenote.app.server.core.nio.startup.AsyncNIOServerBootstrap;
import com.freenote.app.server.core.nio.startup.NIOServerBootstrap;

public interface ServerBootstrap {
    void start(IncomingConnectionHandler handler, ServerSocketConfig config);

    static ServerBootstrap create(String type) {
        switch (type) {
            case "thread-per-connection":
                return new LegacyBootstrap();
            case "nio":
                return new NIOServerBootstrap();
            case "nio2":
                return new AsyncNIOServerBootstrap();
        }
        return new LegacyBootstrap();
    }
}
