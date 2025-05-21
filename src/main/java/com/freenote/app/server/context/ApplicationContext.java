package com.freenote.app.server.context;

import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.handler.impl.MockHandler;

import java.util.Map;

public class ApplicationContext {
    public static final Map<String, URIHandler> HANDLERS = Map.of(
            "/example", new MockHandler()
    );

    public static Map<String, URIHandler> getHandlers() {
        return HANDLERS;
    }
}
