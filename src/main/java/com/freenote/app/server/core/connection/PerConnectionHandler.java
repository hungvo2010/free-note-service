package com.freenote.app.server.core.connection;

import com.freenote.app.server.core.context.ConnectionContext;
import com.freenote.app.server.exceptions.ConnectionException;
import com.freenote.app.server.model.http.HttpUpgradeRequest;

import java.io.IOException;

public interface PerConnectionHandler {
    HttpUpgradeRequest handle(ConnectionContext context) throws ConnectionException, IOException;
}
