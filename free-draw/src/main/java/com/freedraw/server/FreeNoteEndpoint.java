package com.freedraw.server;

import com.freedraw.DraftService;
import com.freedraw.dto.DraftRequestData;
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
import com.freenote.app.server.exceptions.ClientDisconnectException;
import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.frames.control.PongFrame;
import com.freenote.app.server.frames.factory.ServerFrameFactory;
import com.freenote.app.server.handler.impl.CommonEndpointHandlerImpl;
import com.freenote.app.server.model.ws.CommonResponseObject;
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
        System.out.println(JSONUtils.fromJSON(
                """
                        {
                          "content": {
                            "type": 1,
                            "details": {
                              "op": "update",
                              "id": 247,
                              "patch": {
                                "type": "rectangle",
                                "data": {
                                  "id": 247,
                                  "x": 458,
                                  "y": 379.609375,
                                  "width": 599,
                                  "height": -114
                                }
                              }
                            }
                          }
                        }
                        """,
                DraftRequestData.class)
        );

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

            var draftRequest = JSONUtils.fromMap(appMessage.getBody(), DraftRequestData.class);
            log.info("Received DraftRequest: {}", appMessage.getBody().toString());
            if (draftRequest == null) {
                log.error("Received null or invalid DraftRequest");
                webSocketConnection.setResponseFrame(defaultMessageFrame(DEFAULT_MESSAGE_PAYLOAD));
                return;
            }

            var draft = processDraftRequest(draftRequest, appMessage.getType());
            var lastAction = getLastAction(draft);

            var responseData = new DraftResponseData(lastAction);
            log.info("Response: {}", responseData);
            webSocketConnection.setResponseObject(
                    new CommonResponseObject<>(responseData)
            );
            broadcastMessage(draft.getDraftId(), new Connection(webSocketConnection.getOutputStream()),
                    defaultMessageFrame(new AppMessage(lastAction))
            );
        } catch (Exception ex) {
            log.error("Error in application onMessage logic: {}", ex.getMessage());
            webSocketConnection.setResponseFrame(ServerFrameFactory.SERVER.createTextFrame(JSONUtils.toJSONString(DEFAULT_MESSAGE_PAYLOAD)));
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

    private Draft processDraftRequest(DraftRequestData draftRequestData, MessageType type) {
        try {
            return draftService.handleDraftRequest(draftRequestData, type);
        } catch (Exception ex) {
            log.error("Draft Request handle failed: {}", ex.getMessage());
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
