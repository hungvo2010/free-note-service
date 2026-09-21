package com.freenote.app.server.core.startup;

import com.freenote.app.server.core.config.SSLConfig;
import com.freenote.app.server.core.connection.IncomingConnectionHandler;
import com.freenote.app.server.core.legacy.ThreadPerConnectionHandler;
import com.freenote.app.server.core.legacy.startup.LegacyBootstrap;
import com.freenote.app.server.core.nio.NIOIncomingSocketHandler;
import com.freenote.app.server.core.nio.startup.AsyncNIOServerBootstrap;
import com.freenote.app.server.core.nio.startup.NIOServerBootstrap;

public record IoModel(ServerBootstrap acceptLoop, IncomingConnectionHandler handler) {

    public static IoModel of(String serverType, SSLConfig sslConfig) {
        if (sslConfig != null) {
            return new IoModel(LegacyBootstrap.createSSLBootstrap(sslConfig), new ThreadPerConnectionHandler());
        }
        return switch (serverType == null ? "thread-per-connection" : serverType) {
            case "nio" -> new IoModel(new NIOServerBootstrap(), new NIOIncomingSocketHandler());
            case "nio2" -> new IoModel(new AsyncNIOServerBootstrap(), new NIOIncomingSocketHandler());
            default -> new IoModel(new LegacyBootstrap(), new ThreadPerConnectionHandler());
        };
    }
}
