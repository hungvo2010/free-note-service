package com.freenote.app.server.core.nio.state;

import com.freenote.app.server.core.context.ConnectionContext;

public class HandShakeState implements ConnectionState {

    @Override
    public ConnectionState transition(ConnectionContext context) {
        if (context.getReadableContext().isHandshakeComplete()) {
            return new MessageState(context.getReadableContext().getHttpUpgradeRequest());
        }
        return this;
    }
}
