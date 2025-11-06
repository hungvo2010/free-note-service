package com.freenote.app.server.application.models.core;

import com.freenote.annotations.Singleton;
import com.freenote.app.server.connections.Connection;

import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class RoomManager {
    private final ConcurrentHashMap<String, Room> rooms;
    private static volatile RoomManager instance;

    private RoomManager() {
        rooms = new ConcurrentHashMap<>();
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


        public Room getRoomById(String roomId) {
            var draftRoom = this.rooms.putIfAbsent(roomId, new Room());
            if (draftRoom == null) {
                draftRoom = new Room();
                this.rooms.put(roomId, draftRoom);
            }
            return draftRoom;
    }

    public void removeConnectionByOutputStream(OutputStream outputStream) {
        var connection = new Connection(outputStream);
    }
}
