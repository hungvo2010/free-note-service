package com.freenote.app.server.connections;

import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.util.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConnectionPool {
    private final Object lock = new Object();
    private final List<Connection> connections = new ArrayList<>();

    public void addConnection(Connection connection) {
        synchronized (lock) {
            connections.add(connection);
        }
    }

    public void removeConnection(Connection connection) {
        synchronized (lock) {
            connections.remove(connection);
        }
    }

    public void broadcastMessage(WebSocketFrame webSocketFrame) throws IOException {
        synchronized (lock) {
            for (Connection connection : connections) {
                IOUtils.writeOutPut(connection.getSocket().getOutputStream(), webSocketFrame);
            }
        }
    }
}
