package com.freedraw.server;

import com.freedraw.dto.DraftRequestData;
import com.freedraw.dto.DraftResponseData;
import com.freedraw.entities.Draft;
import com.freedraw.entities.DraftAction;
import com.freedraw.models.core.Connection;
import com.freedraw.models.core.Room;
import com.freedraw.models.core.RoomManager;
import com.freedraw.models.enums.DraftRequestType;
import com.freedraw.service.DraftService;
import com.freedraw.utils.FrameUtils;
import com.freenote.annotations.WebSocketEndpoint;
import com.freenote.app.server.core.WebSocketConnection;
import com.freenote.app.server.exceptions.ClientDisconnectException;
import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.frames.control.PongFrame;
import com.freenote.app.server.frames.factory.FrameFactory;
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
    private static final DraftResponseData DEFAULT_MESSAGE_PAYLOAD = new DraftResponseData();
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

    @Override
    public void onMessage(WebSocketConnection webSocketConnection, String message) {
        try {
            var draftRequest = JSONUtils.fromJSON(message, DraftRequestData.class);
            log.info("Received DraftRequest: {}", message);
            if (draftRequest == null) {
                log.error("Received null or invalid DraftRequest");
                webSocketConnection.setResponseFrame(defaultMessageFrame(DEFAULT_MESSAGE_PAYLOAD));
                return;
            }

            var draft = processDraftRequest(draftRequest, draftRequest.getDraftRequestType());
            var lastAction = getLastAction(draft);

            var responseData = new DraftResponseData(draft.getDraftId(), draft.getDraftName(), lastAction.getShapes());
            log.info("Response: {}", JSONUtils.toJSONString(responseData));
            webSocketConnection.setResponseObject(
                    new CommonResponseObject<>(responseData)
            );
            broadcastMessage(draft.getDraftId(), new Connection(webSocketConnection.getOutputStream()),
                    FrameUtils.createApplicationFrame(lastAction)
            );
        } catch (Exception ex) {
            log.error("Error in application onMessage logic: {}", ex);
            webSocketConnection.setResponseFrame(FrameFactory.SERVER.createTextFrame(JSONUtils.toJSONString(DEFAULT_MESSAGE_PAYLOAD)));
        }
    }

    private DraftAction getLastAction(Draft draft) {
        return draft.getActions().get(draft.getActions().size() - 1);
    }

    private WebSocketFrame defaultMessageFrame(DraftResponseData defaultMessagePayload) {
        return FrameUtils.createApplicationFrame(defaultMessagePayload);
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

    private Draft processDraftRequest(DraftRequestData draftRequestData, DraftRequestType type) {
        try {
            return draftService.handleDraftRequest(draftRequestData);
        } catch (Exception ex) {
            log.error("Draft Request handle failed: ", ex);
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


    public static void main(String[] args) {
        log.info(String.valueOf(JSONUtils.fromJSON(
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
                DraftRequestData.class)));
    }
}
