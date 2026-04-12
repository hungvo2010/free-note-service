package com.freedraw.models.core;

import com.freenote.app.server.messages.WebSocketFrame;
import com.freenote.app.server.util.IOUtils;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

public class Room {
    private static final Logger log = LogManager.getLogger(Room.class);
    @Getter
    private final String roomId;
    @Getter
    private final Set<Connection> connections;

    public Room(String draftId) {
        this.roomId = hashDraftId(draftId);
        connections = new HashSet<>();
        log.info("Room created with Room ID: {}", this.roomId);
    }

    private String hashDraftId(String draftId) {
        return draftId;
    }

    public static void main(String[] args) {
        var uuIdVal = UUID.fromString("b4a04191-0547-43d1-a50b-adf676a1e6b0");
        System.out.println(uuIdVal.hashCode());
        System.out.println("b4a04191-0547-43d1-a50b-adf676a1e6b0".hashCode());
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
