package com.freenote.app.server.core.connection;

import com.freenote.app.server.core.context.ConnectionContext;
import com.freenote.app.server.core.legacy.ThreadPerConnectionHandler;
import com.freenote.app.server.core.nio.NIOIncomingSocketHandler;
import com.freenote.app.server.exceptions.ConnectionException;

public interface IncomingConnectionHandler {
    void handle(ConnectionContext context) throws ConnectionException;

    static IncomingConnectionHandler fromType(String connectionType) {
        switch (connectionType) {
            case "thread-per-connection":
                return new ThreadPerConnectionHandler();
            case "nio":
                return new NIOIncomingSocketHandler();
            case "nio2":
                return new NIOIncomingSocketHandler();
        }
        return new ThreadPerConnectionHandler();
    }
}
