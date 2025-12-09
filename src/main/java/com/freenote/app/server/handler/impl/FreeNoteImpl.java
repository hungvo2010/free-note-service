package com.freenote.app.server.handler.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freenote.annotations.URIHandlerImplementation;
import com.freenote.app.server.application.CoreDraftProcessor;
import com.freenote.app.server.application.factory.ApplicationFrameFactory;
import com.freenote.app.server.application.factory.ServerApplicationFrameFactory;
import com.freenote.app.server.application.models.common.MessagePayload;
import com.freenote.app.server.application.models.core.Room;
import com.freenote.app.server.application.models.core.RoomManager;
import com.freenote.app.server.application.models.request.DraftRequest;
import com.freenote.app.server.connections.Connection;
import com.freenote.app.server.connections.WebSocketConnection;
import com.freenote.app.server.exceptions.ClientDisconnectException;
import com.freenote.app.server.exceptions.MessagePayloadParsingException;
import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.util.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@URIHandlerImplementation("/freeNote")
public class FreeNoteImpl extends CommonHandlerImpl {
    private static final Logger log = LogManager.getLogger(FreeNoteImpl.class);
    private static final MessagePayload DEFAULT_MESSAGE_PAYLOAD = new MessagePayload();
    private final ObjectMapper objMapper = new ObjectMapper();
    private final CoreDraftProcessor coreDraftProcessor = new CoreDraftProcessor();
    private final ApplicationFrameFactory applicationFrameFactory = new ServerApplicationFrameFactory();
    private final RoomManager roomManager = RoomManager.getInstance();

    private void removeConnectionByInputStream(OutputStream outputStream) {
        roomManager.removeConnectionByInputStream(outputStream);
    }

    private void removeConnection(Room targetRoom, Connection newConnection) {
        targetRoom.remove(newConnection);
    }

    private WebSocketFrame doApplicationLogic(DraftRequest draftRequest) {
        try {
            var responsePayload = handleClientMessage(draftRequest);
            return applicationFrameFactory.createApplicationFrame(responsePayload);
        } catch (MessagePayloadParsingException ex) {
            log.error("Failed to parse MessagePayload: {}", ex.getMessage());
            return applicationFrameFactory.createApplicationFrame(DEFAULT_MESSAGE_PAYLOAD);
        } catch (Exception ex) {
            log.error("Error in application logic: {}", ex.getMessage());
            throw ex;
        }
    }

    private void broadcastMessage(String roomId, Connection newConnection, WebSocketFrame clientResponse) {
        var targetRoom = roomManager.getRoomById(roomId);
        try {
            targetRoom.addMember(newConnection);
            var connectionsToBroadcast = targetRoom.getConnectionsInRoomToBroadcast(List.of(newConnection));
            targetRoom.broadCastMessage(connectionsToBroadcast, clientResponse);
        } catch (Exception e) {
            removeConnection(targetRoom, newConnection);
        }
    }

    private MessagePayload handleClientMessage(DraftRequest draftRequest) {
        try {
            return coreDraftProcessor.processDraft(draftRequest);
        } catch (Exception ex) {
            log.error("Error handling client message: ", ex);
            return DEFAULT_MESSAGE_PAYLOAD;
        }
    }

    private DraftRequest convertToDraftRequest(MessagePayload messagePayload) {
        var actualPayload = messagePayload.getPayload();
        return objMapper.convertValue(actualPayload, DraftRequest.class);
    }

    private MessagePayload getMessagePayload(String rawPayload) {
        try {
            return objMapper.readValue(rawPayload, MessagePayload.class);
        } catch (IOException e) {
            log.error("Error parsing MessagePayload from DataFrame", e.getCause());
            throw new MessagePayloadParsingException("Error parsing MessagePayload from DataFrame", e);
        }
    }

    @Override
    public void onClose(WebSocketConnection webSocketConnection, int code, String reason, boolean remote) {
        removeConnectionByInputStream(webSocketConnection.getOutputStream());
        throw new ClientDisconnectException("Client sent CLOSE frame");

    }

    @Override
    public void onError(WebSocketConnection webSocketConnection, Exception exception) {
        log.error("Error handling input stream: ", exception);

    }

    @Override
    public void onMessage(WebSocketConnection webSocketConnection, String message) {
        try {
            var messagePayload = getMessagePayload(message);
            var draftRequest = convertToDraftRequest(messagePayload);
            var clientResponse = doApplicationLogic(draftRequest);

            IOUtils.writeOutPut(webSocketConnection.getOutputStream(), clientResponse);
            broadcastMessage(draftRequest.getDraftId(), new Connection(webSocketConnection.getOutputStream()), clientResponse);
        } catch (Exception ex) {
            log.error("Error in application onMessage logic: {}", ex);
        }


    }
}
