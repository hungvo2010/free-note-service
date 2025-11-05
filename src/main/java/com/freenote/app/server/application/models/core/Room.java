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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Room {
    private static final Logger log = LogManager.getLogger(Room.class);
    @Getter
    private final String roomId;
    private final List<Connection> connections;

    public Room() {
        this.roomId = UUID.randomUUID().toString();
        connections = new ArrayList<>();
        log.info("Room created with Room ID: {}", roomId);
    }

    public void addMember(Connection connection) {
        connections.add(connection);
    }

    public void broadCastMessage(Object data) {
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
}
