package com.freedraw.endpoint;

import com.freedraw.dto.DraftRequestData;
import com.freedraw.dto.DraftResponseContent;
import com.freedraw.dto.DraftResponseData;
import com.freedraw.entities.Draft;
import com.freedraw.entities.DraftAction;
import com.freedraw.models.core.Connection;
import com.freedraw.models.core.Room;
import com.freedraw.models.core.RoomRegistry;
import com.freedraw.repository.InMemDraftRepositoryImpl;
import com.freedraw.service.DraftService;
import com.freenote.annotations.WebSocketEndpoint;
import com.freenote.app.server.core.connection.WebSocketConnection;
import com.freenote.app.server.exceptions.ClientDisconnectException;
import com.freenote.app.server.frames.base.ControlFrame;
import com.freenote.app.server.handler.endpoint.AbstractEndpointHandler;
import com.freenote.app.server.messages.ws.WebSocketFrame;
import com.freenote.app.server.model.ws.CommonResponseObject;
import com.freenote.app.server.util.FrameUtil;
import com.freenote.app.server.util.JSONUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.util.List;

@WebSocketEndpoint("/freeNote")
public class FreeNoteEndpoint extends AbstractEndpointHandler {
    private static final Logger log = LogManager.getLogger(FreeNoteEndpoint.class);
    private static final DraftResponseData DEFAULT_MESSAGE_PAYLOAD = new DraftResponseData();
    private final DraftService draftService = new DraftService(new InMemDraftRepositoryImpl());
    private final RoomRegistry roomRegistry = RoomRegistry.getInstance();

    @Override
    public void onData(WebSocketConnection webSocketConnection, String message) {
        try {
            var draftRequest = JSONUtils.fromJSON(message, DraftRequestData.class);
            log.info("Received DraftRequest: {}", message);
            if (draftRequest == null) {
                webSocketConnection.setResponseObject(new CommonResponseObject<>(DEFAULT_MESSAGE_PAYLOAD));
                return;
            }

            var draft = draftService.handleDraftRequest(draftRequest);
            var responseData = buildResponseAction(draft, draftRequest);
            webSocketConnection.setResponseObject(
                    new CommonResponseObject<>(responseData)
            );
            broadcastMessage(draft.getDraftId(), Connection.from(webSocketConnection),
                    FrameUtil.createApplicationFrame(responseData)  // Use responseData instead of lastAction
            );
        } catch (Exception ex) {
            log.error("Error in application onMessage logic: {}", ex.getMessage());
            webSocketConnection.setResponseObject(new CommonResponseObject<>(DEFAULT_MESSAGE_PAYLOAD));
        }
    }

    private DraftResponseData buildResponseAction(Draft draft, DraftRequestData draftRequest) {
        var lastAction = getLastAction(draft);
        var responseContent = new DraftResponseContent(lastAction.getShapes());

        var responseData = DraftResponseData.builder()
                .draftId(draft.getDraftId())
                .draftName(draft.getDraftName())
                .data(responseContent)
                .requestType(draftRequest.getDraftRequestType())
                .senderId(draftRequest.getSenderId())
                .build();

        log.info("Response: {}", JSONUtils.toJSONString(responseData));
        return responseData;
    }

    @Override
    public void onClose(WebSocketConnection webSocketConnection, int code, String reason, boolean remote) {
        roomRegistry.removeConnection(new Connection(webSocketConnection.getOutputStream()));
        throw new ClientDisconnectException("Client sent CLOSE frame");
    }

    @Override
    public void onError(WebSocketConnection webSocketConnection, Exception exception) {
        log.error("Error handling input stream: ", exception);

    }

    @Override
    public void onPing(WebSocketConnection webSocketConnection, ByteBuffer payload) {
        webSocketConnection.setResponseFrame(ControlFrame.pong());
    }

    private DraftAction getLastAction(Draft draft) {
        return draft.getActions().get(draft.getActions().size() - 1);
    }

    private void broadcastMessage(String roomId, Connection newConnection, WebSocketFrame clientResponse) {
        var targetRoom = roomRegistry.getRoomById(roomId);
        try {
            targetRoom.addMember(newConnection);
            var connectionsToBroadcast = targetRoom.getConnectionsInRoomToBroadcast(List.of(newConnection));
            targetRoom.broadCastMessage(connectionsToBroadcast, clientResponse);
        } catch (Exception e) {
            removeConnection(targetRoom, newConnection);
        }
    }

    private void removeConnection(Room targetRoom, Connection newConnection) {
        targetRoom.remove(newConnection);
    }

}
