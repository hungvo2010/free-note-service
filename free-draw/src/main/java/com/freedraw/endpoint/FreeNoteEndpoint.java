package com.freedraw.endpoint;

import com.freedraw.dto.DraftRequestData;
import com.freedraw.dto.DraftResponseData;
import com.freedraw.entities.Draft;
import com.freedraw.entities.DraftAction;
import com.freedraw.models.core.Connection;
import com.freedraw.models.core.Room;
import com.freedraw.models.core.RoomManager;
import com.freedraw.service.DraftService;
import com.freenote.annotations.WebSocketEndpoint;
import com.freenote.app.server.core.WebSocketConnection;
import com.freenote.app.server.exceptions.ClientDisconnectException;
import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.frames.control.PongFrame;
import com.freenote.app.server.frames.factory.FrameFactory;
import com.freenote.app.server.handler.impl.CommonEndpointHandlerImpl;
import com.freenote.app.server.model.ws.CommonResponseObject;
import com.freenote.app.server.util.FrameUtil;
import com.freenote.app.server.util.JSONUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;

@WebSocketEndpoint("/freeNote")
public class FreeNoteEndpoint extends CommonEndpointHandlerImpl {
    private static final Logger log = LogManager.getLogger(FreeNoteEndpoint.class);
    private static final DraftResponseData DEFAULT_MESSAGE_PAYLOAD = new DraftResponseData();
    private final DraftService draftService = new DraftService();
    private final RoomManager roomManager = RoomManager.getInstance();

    @Override
    public void onClose(WebSocketConnection webSocketConnection, int code, String reason, boolean remote) {
        removeConnectionByInputStream(webSocketConnection.getOutputWrapper().outputStream());
        throw new ClientDisconnectException("Client sent CLOSE frame");

    }

    @Override
    public void onError(WebSocketConnection webSocketConnection, Exception exception) {
        log.error("Error handling input stream: ", exception);

    }

    @Override
    public void onPing(WebSocketConnection webSocketConnection, ByteBuffer payload) {
        webSocketConnection.setResponseFrame(new PongFrame());
    }

    @Override
    public void onControl(WebSocketConnection webSocketConnection, ByteBuffer payload) {
    }

    private DraftAction getLastAction(Draft draft) {
        return draft.getActions().get(draft.getActions().size() - 1);
    }

    @Override
    public void onData(WebSocketConnection webSocketConnection, String message) {
        try {
            var draftRequest = JSONUtils.fromJSON(message, DraftRequestData.class);
            log.info("Received DraftRequest: {}", message);
            if (draftRequest == null) {
                log.error("Received null or invalid DraftRequest");
                webSocketConnection.setResponseFrame(FrameFactory.SERVER.createTextFrame(JSONUtils.toJSONString(DEFAULT_MESSAGE_PAYLOAD)));
                return;
            }

            var draft = draftService.handleDraftRequest(draftRequest);
            var lastAction = getLastAction(draft);

            var responseData = new DraftResponseData(draft.getDraftId(), draft.getDraftName(), lastAction.getShapes());
            responseData.setRequestType(draftRequest.getDraftRequestType());
            responseData.setSenderId(draftRequest.getSenderId());
            log.info("Response: {}", JSONUtils.toJSONString(responseData));

            // Send response to the sender
            webSocketConnection.setResponseObject(
                    new CommonResponseObject<>(responseData)
            );

            // Broadcast the SAME format to other clients in the room
            broadcastMessage(draft.getDraftId(), new Connection(webSocketConnection.getOutputWrapper().outputStream()),
                    FrameUtil.createApplicationFrame(responseData)  // Use responseData instead of lastAction
            );
        } catch (Exception ex) {
            log.error("Error in application onMessage logic: {}", ex.getMessage());
            webSocketConnection.setResponseFrame(FrameFactory.SERVER.createTextFrame(JSONUtils.toJSONString(DEFAULT_MESSAGE_PAYLOAD)));
        }
    }

    private void removeConnectionByInputStream(OutputStream outputStream) {
        roomManager.removeConnectionByInputStream(outputStream);
    }

    private void removeConnection(Room targetRoom, Connection newConnection) {
        targetRoom.remove(newConnection);
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


}
