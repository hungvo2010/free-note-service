package com.freenote.app.server.application.models.core;

import com.freenote.annotations.Singleton;
import com.freenote.app.server.application.models.request.DraftRequest;
import com.freenote.app.server.connections.Connection;

import java.util.HashMap;
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

    public void addConnection(String draftId, DraftRequest draftRequest) {
        var requestMeta = draftRequest.getMetaData();
        var connection = new Connection(
                requestMeta.getSocket(),
                requestMeta.getIpAddress(),
                requestMeta.getPort()
        );
        var draftRoom = this.rooms.putIfAbsent(draftId, new Room());
        if (draftRoom != null) {
            draftRoom.addMember(connection);
        }
    }

    public void broadcastToRoom(String draftId, DraftAction returnAction) {
        var room = this.rooms.get(draftId);
        room.broadCastMessage(returnAction);
    }
}
