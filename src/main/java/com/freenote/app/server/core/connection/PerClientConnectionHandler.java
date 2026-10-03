package com.freenote.app.server.core.connection;

import com.freenote.app.server.core.event.ConnectionEvent;
import com.freenote.app.server.exceptions.WebSocketException;
import com.freenote.app.server.handshaker.http.HttpUgrade;

import java.io.IOException;

public interface PerClientConnectionHandler {
    HttpUgrade.HttpUpgradeRequest handle(ConnectionEvent event) throws WebSocketException.ConnectionException, IOException;
}
