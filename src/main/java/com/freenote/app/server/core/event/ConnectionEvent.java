package com.freenote.app.server.core.event;

import com.freenote.app.server.core.nio.state.ConnectionState;
import com.freenote.app.server.model.ws.NetworkRequestData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class ConnectionEvent {
    private final NetworkRequestData networkRequestData;
    private final ConnectionState state;
}
