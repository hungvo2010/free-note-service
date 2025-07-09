package com.freenote.app.server.context;

import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.handler.impl.EchoHandler;

import java.util.Map;

public class ApplicationContext {
    public static final Map<String, URIHandler> HANDLERS = Map.of(
            "/example", new EchoHandler()
    );

    public static Map<String, URIHandler> getHandlers() {
        return HANDLERS;
    }
}
