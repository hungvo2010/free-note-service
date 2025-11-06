package com.freenote.app.server.application.models.core;

import com.freenote.annotations.Singleton;
import com.freenote.app.server.connections.Connection;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class RoomManager {
    private final Map<String, Room> rooms;
    private static volatile RoomManager instance;

    private RoomManager() {
        rooms = new HashMap<>();
    }

    public static RoomManager getInstance() {
        if (instance == null) {
            synchronized (RoomManager.class) {
                if (instance == null) {
                    instance = new RoomManager();
                }
            }
        }
        return instance;
    }

    public Connection addConnection(String draftId, OutputStream outputStream) {
        var connection = new Connection(outputStream);
        var draftRoom = this.rooms.putIfAbsent(draftId, new Room());
        if (draftRoom != null) {
            draftRoom.addMember(connection);
        }
        return connection;
    }

    public void broadcastToRoom(String roomId, Object returnAction, Connection excludeConnection) {
        var room = this.rooms.get(roomId);
        room.broadCastMessage(getConnectionsInRoomToBroadcast(roomId, excludeConnection), returnAction);
    }

    public List<Connection> getConnectionsInRoomToBroadcast(String roomId, Connection excludeConnection) {
        var room = this.rooms.get(roomId);
        return room
                .getConnections()
                .stream()
                .filter(connection -> connection != excludeConnection)
                .toList();
    }
}
