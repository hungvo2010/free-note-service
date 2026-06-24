package com.freenote.app.server.core.nio.state;

import com.freenote.app.server.core.connection.IncomingConnectionHandler;
import com.freenote.app.server.core.context.ConnectionContext;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

@Log4j2
public class HandShakeState implements ConnectionState {

    @Override
    public ConnectionState handle(IncomingConnectionHandler handler, ConnectionContext context) throws IOException {
        try {
            handler.handle(context);
            if (context.getReadableContext().isHandshakeComplete()) {
                return new MessageState(context.getReadableContext().getHttpUpgradeRequest());
            }
            return this;
        } catch (Exception e) {
            log.error("Handshake failed: ", e);
            context.getNetworkRequestData().close();
            return null;
        }
    }
}
