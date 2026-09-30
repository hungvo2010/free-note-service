package com.freenote.app.server.core;

import com.freenote.app.server.config.SSLConfig;
import com.freenote.app.server.core.connection.PerClientConnectionHandler;
import com.freenote.app.server.core.legacy.ThreadPerConnectionHandler;
import com.freenote.app.server.core.legacy.startup.LegacyBootstrap;
import com.freenote.app.server.core.nio.NIOIncomingSocketHandler;
import com.freenote.app.server.core.nio.startup.AsyncNIOServerBootstrap;
import com.freenote.app.server.core.nio.startup.NIOServerBootstrap;
import com.freenote.app.server.core.startup.ServerBootstrap;
import com.freenote.app.server.endpoints.EndpointResolver;

public record IoModel(ServerBootstrap acceptLoop, PerClientConnectionHandler handler) {

    public static IoModel of(String serverType, SSLConfig sslConfig, EndpointResolver endpointResolver) {
        if (sslConfig != null) {
            return new IoModel(LegacyBootstrap.createSSLBootstrap(sslConfig), new ThreadPerConnectionHandler(endpointResolver));
        }
        return switch (serverType == null ? "thread-per-connection" : serverType) {
            case "nio" -> new IoModel(new NIOServerBootstrap(), new NIOIncomingSocketHandler(endpointResolver));
            case "nio2" -> new IoModel(new AsyncNIOServerBootstrap(), new NIOIncomingSocketHandler(endpointResolver));
            default -> new IoModel(new LegacyBootstrap(), new ThreadPerConnectionHandler(endpointResolver));
        };
    }
}
