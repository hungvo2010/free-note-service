package com.freedraw.models.core;

import com.freenote.app.server.connections.Connection;
import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.util.IOUtils;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Room {
    private static final Logger log = LogManager.getLogger(Room.class);
    @Getter
    private final String roomId;
    @Getter
    private final Set<Connection> connections;

    public Room(String roomId) {
        this.roomId = roomId;
        connections = new HashSet<>();
        log.info("Room created with Room ID: {}", this.roomId);
    }

    public void addMember(Connection connection) {
        connections.add(connection);
    }

    public void broadCastMessage(List<Connection> connections, WebSocketFrame data) {
        log.info("Broadcasting message to {} members", connections.size());
        for (Connection connection : connections) {
            sendMember(connection, data);
        }
    }

    private void sendMember(Connection connection, WebSocketFrame data) {
        try {
            IOUtils.writeOutPut(connection.getOutputStream(), data);
        } catch (IOException e) {
            log.error("Error broadcasting to member: {}", e.getMessage());
        }
    }


    public List<Connection> getConnectionsInRoomToBroadcast(List<Connection> excludeConnections) {
        return this
                .getConnections()
                .stream()
                .filter(connection -> !excludeConnections.contains(connection) && connection.isOpen())
                .toList();
    }

    public void remove(Connection newConnection) {
        for (Iterator<Connection> iterator = connections.iterator(); iterator.hasNext(); ) {
            Connection connection = iterator.next();
            if (connection.equals(newConnection)) {
                iterator.remove();
                connection.close();
                log.info("Connection removed from room: {}", roomId);
                break;
            }
        }
    }
}
