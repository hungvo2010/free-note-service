package com.freenote.app.server.application.models.core;

import com.freenote.app.server.application.factory.ServerApplicationFrameFactory;
import com.freenote.app.server.application.models.common.MessagePayload;
import com.freenote.app.server.connections.Connection;
import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.util.IOUtils;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashSet;
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


    public void addConnection(Connection connection) {
        this.addMember(connection);
    }


    public void addMember(Connection connection) {
        connections.add(connection);
    }

    public void broadCastMessage(List<Connection> connections, Object data) {
        log.info("Broadcasting message to {} members", connections.size());
        for (Connection connection : connections) {
            broadcastToMember(connection, createFrame(data));
        }
    }


    private WebSocketFrame createFrame(Object data) {
        return new ServerApplicationFrameFactory().createApplicationFrame(new MessagePayload(data));
    }


    private void broadcastToMember(Connection connection, WebSocketFrame data) {
        try {
            IOUtils.writeOutPut(connection.getOutputStream(), data);
        } catch (IOException e) {
            log.error("Error broadcasting to member: {}", e.getMessage());
        }
    }


    public List<Connection> getConnectionsInRoomToBroadcast(String roomId, List<Connection> excludeConnections) {
        return this
                .getConnections()
                .stream()
                .filter(connection -> !excludeConnections.contains(connection) && connection.isOpen())
                .toList();
    }
}
