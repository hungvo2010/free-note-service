package com.freenote.app.server.application.models.core;

import com.freenote.annotations.Singleton;
import com.freenote.app.server.connections.Connection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class RoomManager {
    private static final Logger log = LogManager.getLogger(RoomManager.class);
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
        var draftRoom = this.rooms.computeIfAbsent(roomId, Room::new);
        log.info("Getting room with ID: {} - {}", roomId, draftRoom);
        return draftRoom;
    }
}
