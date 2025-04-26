package com.freenote.app.server.startup;

import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.handler.impl.MockHandler;

import java.util.Map;

public class StartupProcess {
    public static final Map<String, URIHandler> HANDLERS = Map.of(
            "/example", new MockHandler()
    );
}
