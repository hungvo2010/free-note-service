package com.freenote.app.server.application.models.core;

import com.freenote.app.server.connections.Connection;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Room {
    @Getter
    private String roomId;
    private List<Connection> connections = new ArrayList<>();

    public Room() {
        this.roomId = UUID.randomUUID().toString();
        connections = new ArrayList<>();
    }

    public void addMember(Connection connection) {
        connections.add(connection);
    }

    public void broadCastMessage() {
        for (Connection connection : connections) {
            System.out.println("Member: " + connection.getSourceIp());
        }
    }
}
