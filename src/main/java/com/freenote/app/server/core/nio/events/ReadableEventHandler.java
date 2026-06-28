package com.freenote.app.server.core.nio.events;

import com.freenote.app.server.core.context.ReadableContext;

public interface ReadableEventHandler {
    void handle(ReadableContext readableContext) throws Exception;
}
