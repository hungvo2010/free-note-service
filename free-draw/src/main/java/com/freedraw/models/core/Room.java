package com.freedraw.models.core;

import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class Room {
    private static final Logger log = LogManager.getLogger(Room.class);
    @Getter
    private final String roomId;
    @Getter
    private final Set<AppConnection> connections;

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

    public void addMember(AppConnection connection) {
        connections.add(connection);
    }

    public List<AppConnection> getConnectionsInRoomToBroadcast(List<AppConnection> excludeConnections) {
        return this
                .getConnections()
                .stream()
                .filter(connection -> isEligibleForBroadcast(excludeConnections, connection))
                .toList();
    }

    private boolean isEligibleForBroadcast(List<AppConnection> excludeConnections, AppConnection connection) {
        return !excludeConnections.contains(connection) && connection.isOpen();
    }

    public void remove(AppConnection newConnection) {
        for (Iterator<AppConnection> iterator = connections.iterator(); iterator.hasNext(); ) {
            AppConnection connection = iterator.next();
            if (Objects.equals(connection, newConnection)) {
                iterator.remove();
                connection.close();
                log.info("Connection sent event, skip broadcast: {}", roomId);
                break;
            }
        }
    }
}
