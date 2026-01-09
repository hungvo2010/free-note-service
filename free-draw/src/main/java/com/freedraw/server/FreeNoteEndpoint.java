package com.freedraw.server;

import com.freedraw.DraftService;
import com.freedraw.dto.DraftRequest;
import com.freedraw.dto.DraftResponseData;
import com.freedraw.entities.Draft;
import com.freedraw.entities.DraftAction;
import com.freedraw.factory.ApplicationFrameFactory;
import com.freedraw.models.common.AppMessage;
import com.freedraw.models.core.Connection;
import com.freedraw.models.core.Room;
import com.freedraw.models.core.RoomManager;
import com.freedraw.models.enums.MessageType;
import com.freenote.annotations.WebSocketEndpoint;
import com.freenote.app.server.core.WebSocketConnection;
import com.freenote.app.server.data.ws.ResponseObject;
import com.freenote.app.server.exceptions.ClientDisconnectException;
import com.freenote.app.server.exceptions.MessagePayloadParsingException;
import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.frames.control.PongFrame;
import com.freenote.app.server.handler.impl.CommonEndpointHandlerImpl;
import com.freenote.app.server.util.JSONUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;

@WebSocketEndpoint("/freeNote")
public class FreeNoteEndpoint extends CommonEndpointHandlerImpl {
    private static final Logger log = LogManager.getLogger(FreeNoteEndpoint.class);
    private static final AppMessage DEFAULT_MESSAGE_PAYLOAD = new AppMessage();
    private final DraftService draftService = new DraftService();
    private final RoomManager roomManager = RoomManager.getInstance();

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
    public void onPing(WebSocketConnection webSocketConnection, ByteBuffer payload) {
        webSocketConnection.setResponseFrame(new PongFrame());
    }

    @Override
    public void onControl(WebSocketConnection webSocketConnection, ByteBuffer payload) {

    }

    public static void main(String[] args) {
        System.out.println(JSONUtils.fromJSON("{requestType=2, content={type=1, details={op=update, id=247, patch={type=rectangle, data={id=247, x=458, y=379.609375, width=599, height=-114}}}}}", DraftRequest.class));

    }

    @Override
    public void onMessage(WebSocketConnection webSocketConnection, String message) {
        try {
            var appMessage = JSONUtils.fromJSON(message, AppMessage.class);
            if (appMessage == null) {
                log.error("Received null or invalid MessagePayload");
                webSocketConnection.setResponseFrame(defaultMessageFrame(DEFAULT_MESSAGE_PAYLOAD));
                return;
            }

            var draftRequest = JSONUtils.fromMap(appMessage.getBody(), DraftRequest.class);
            log.info("Received DraftRequest: {}", appMessage.getBody().toString());
            if (draftRequest == null) {
                log.error("Received null or invalid DraftRequest");
                webSocketConnection.setResponseFrame(defaultMessageFrame(DEFAULT_MESSAGE_PAYLOAD));
                return;
            }

            var draft = processDraftRequest(draftRequest, appMessage.getType());
            var lastAction = getLastAction(draft);
            webSocketConnection.setResponse(
                    new ResponseObject<>(0, new DraftResponseData(lastAction))
            );
            broadcastMessage(draft.getDraftId(), new Connection(webSocketConnection.getOutputStream()),
                    defaultMessageFrame(new AppMessage(lastAction))
            );
        } catch (Exception ex) {
            log.error("Error in application onMessage logic: ", ex);
        }
    }

    private DraftAction getLastAction(Draft draft) {
        return draft.getActions().get(draft.getActions().size() - 1);
    }

    private WebSocketFrame defaultMessageFrame(AppMessage defaultMessagePayload) {
        return ApplicationFrameFactory.SERVER.createApplicationFrame(defaultMessagePayload);
    }

    @Override
    public void onData(WebSocketConnection webSocketConnection, String message) {

    }

    private void removeConnectionByInputStream(OutputStream outputStream) {
        roomManager.removeConnectionByInputStream(outputStream);
    }

    private void removeConnection(Room targetRoom, Connection newConnection) {
        targetRoom.remove(newConnection);
    }

    private Draft processDraftRequest(DraftRequest draftRequest, MessageType type) {
        try {
            return draftService.handleDraftRequest(draftRequest, type);
        } catch (MessagePayloadParsingException ex) {
            log.error("Failed to parse MessagePayload: {}", ex.getMessage());
            return new Draft();
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
}
