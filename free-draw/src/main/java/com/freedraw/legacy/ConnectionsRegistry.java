package com.freedraw.legacy;

import com.freedraw.dto.DraftResponseData;
import com.freedraw.models.core.AppConnection;
import com.freedraw.resources.RedisClient;
import com.freenote.app.server.model.ws.NetworkRequestData;
import com.freenote.app.server.frames.factory.ServerFrameFactory;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ConnectionsRegistry {
    private static final Logger log = LogManager.getLogger(ConnectionsRegistry.class);
    private static final ServerFrameFactory FRAME_FACTORY = new ServerFrameFactory();
    private static final Cache<String, AppConnection> MEMBERS = CacheBuilder.newBuilder()
            .expireAfterWrite(90, TimeUnit.SECONDS)
            .build();
    private static final Cache<NetworkRequestData, String> MAP_NETWORK_REQUEST_SENDER_ID = CacheBuilder.newBuilder()
            .expireAfterWrite(90, TimeUnit.SECONDS)
            .build();

    public static void register(AppConnection connection) {
        var senderId = connection.getSenderId();
        if (senderId == null || senderId.isEmpty()) {
            return;
        }
        MEMBERS.put(senderId, connection);
        MAP_NETWORK_REQUEST_SENDER_ID.put(connection.getSocketConnection().getNetworkRequestData(), senderId);
        RedisClient.claimMember(senderId);
    }

    public static void unregister(AppConnection connection) {
        MEMBERS.asMap().values().remove(connection);
        MAP_NETWORK_REQUEST_SENDER_ID.invalidate(connection.getSocketConnection().getNetworkRequestData());
    }

    public static void sendToMember(DraftResponseData message) {
        var recipientId = message.getRecipientId();
        if (recipientId == null) {
            return;
        }
        var connection = MEMBERS.getIfPresent(recipientId);
        if (connection == null) {
            return;
        }
        try {
            connection.writeData(FRAME_FACTORY.createApplicationFrame(message));
        } catch (IOException e) {
            log.error("Failed to deliver message to recipient {}", message.getRecipientId(), e);
        }
    }

    public static void refresh(NetworkRequestData networkRequestData) {
        var senderId = MAP_NETWORK_REQUEST_SENDER_ID.getIfPresent(networkRequestData);
        if (senderId == null) {
            return;
        }
        MAP_NETWORK_REQUEST_SENDER_ID.put(networkRequestData, senderId);
        var connection = MEMBERS.getIfPresent(senderId);
        if (connection != null) {
            MEMBERS.put(senderId, connection);
        }
        RedisClient.claimMember(senderId);
    }
}
