package com.freenote.app.server.core.context;

import com.freenote.app.server.model.ws.NetworkRequestData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class ConnectionContext {
    private final NetworkRequestData networkRequestData;
    private final ReadableContext readableContext;
}
