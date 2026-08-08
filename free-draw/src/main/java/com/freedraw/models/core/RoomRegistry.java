package com.freedraw.models.core;

import com.freenote.annotations.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class RoomRegistry {
    private static final Logger log = LogManager.getLogger(RoomRegistry.class);
    private final ConcurrentHashMap<String, Room> rooms;
    private static volatile RoomRegistry instance;

    private RoomRegistry() {
        rooms = new ConcurrentHashMap<>();
    }

    public static RoomRegistry getInstance() {
        if (instance == null) {
            synchronized (RoomRegistry.class) {
                if (instance == null) {
                    instance = new RoomRegistry();
                }
            }
        }
        return instance;
    }
    public void removeConnection(Connection connection) {
        if (connection == null) return;
        rooms.values().forEach(room -> room.remove(connection));
        log.debug("Connection removed from all relevant rooms.");
    }

    public Room getRoomById(String roomId) {
        var draftRoom = this.rooms.computeIfAbsent(roomId, Room::new);
        log.info("Getting room with ID: {}", draftRoom.getRoomId());
        return draftRoom;
    }
}
