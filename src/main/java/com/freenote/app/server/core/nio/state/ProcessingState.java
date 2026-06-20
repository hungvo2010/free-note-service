package com.freenote.app.server.core.nio.state;

import com.freenote.app.server.core.connection.IncomingConnectionHandler;
import com.freenote.app.server.core.context.ConnectionContext;
import com.freenote.app.server.exceptions.ClientDisconnectException;
import com.freenote.app.server.model.http.HttpUpgradeRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

@AllArgsConstructor
@Log4j2
@Getter
public class ProcessingState implements ConnectionState {
    private final HttpUpgradeRequest request;

    @Override
    public ConnectionState handle(IncomingConnectionHandler handler, ConnectionContext context) throws IOException {
        try {
            handler.handle(context);
            if (context.getNetworkRequestData().isClosed()) {
                return null;
            }
            return this;
        } catch (ClientDisconnectException e) {
            context.getNetworkRequestData().close();
            return null;
        } catch (Exception e) {
            context.getNetworkRequestData().close();
            log.warn("[ProcessingState] Exception in handling new messages: {}", e.getMessage());
            return null;
        }
    }
}
