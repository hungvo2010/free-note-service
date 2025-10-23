package com.freenote.app.server.application.models.core;

import com.freenote.app.server.connections.Connection;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Log4j2
public class Room {
    @Getter
    private final String roomId;
    private List<Connection> connections;

    public Room() {
        this.roomId = UUID.randomUUID().toString();
        connections = new ArrayList<>();
    }

    public void addMember(Connection connection) {
        connections.add(connection);
    }

    public void broadCastMessage() {
        for (Connection connection : connections) {
            log.info("Member: {}", connection.getSourceIp());
        }
    }
}
