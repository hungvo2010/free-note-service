package com.freedraw.endpoint;

import com.freedraw.dto.DraftRequestData;
import com.freedraw.dto.DraftResponseContent;
import com.freedraw.dto.DraftResponseData;
import com.freedraw.dto.HeartbeatMsg;
import com.freedraw.entities.Draft;
import com.freedraw.entities.DraftAction;
import com.freedraw.legacy.ConnectionsRegistry;
import com.freedraw.models.core.AppConnection;
import com.freedraw.models.core.RoomRegistry;
import com.freedraw.repository.InMemDraftRepositoryImpl;
import com.freedraw.resources.RedisClient;
import com.freedraw.service.DraftService;
import com.freenote.annotations.WebSocketEndpoint;
import com.freenote.app.server.core.model.connection.WebSocketConnection;
import com.freenote.app.server.exceptions.ClientDisconnectException;
import com.freenote.app.server.frames.base.ControlFrame;
import com.freenote.app.server.model.enums.MsgType;
import com.freenote.app.server.routes.endpoint.AbstractEndpointHandler;
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
    private DraftService draftService = new DraftService(new InMemDraftRepositoryImpl());
    private final RoomRegistry roomRegistry = RoomRegistry.getInstance();

    @Override
    public void onData(WebSocketConnection webSocketConnection, String message) {
        try {
            var heartbeat = JSONUtils.fromJSON(message, HeartbeatMsg.class);
            if (heartbeat != null && heartbeat.getMsgType() == MsgType.PING) {
                log.info("Received Heartbeat PING");
                ConnectionsRegistry.refresh(webSocketConnection.getNetworkRequestData());
                heartbeat.setMsgType(MsgType.PONG);
                webSocketConnection.setResponseFrame(FrameUtil.createApplicationFrame(heartbeat));
                return;
            }

            var draftRequest = JSONUtils.fromJSON(message, DraftRequestData.class);
            log.info("Received DraftRequest: {}", message);
            if (draftRequest == null) {
                webSocketConnection.setAppResponseData(DEFAULT_MESSAGE_PAYLOAD);
                return;
            }

            var draft = draftService.handleDraftRequest(draftRequest);
            var responseData = buildResponseAction(draft, draftRequest);
            webSocketConnection.setAppResponseData(responseData);
            var connection = new AppConnection(webSocketConnection, draftRequest.getSenderId());
            ConnectionsRegistry.register(connection);
            broadcastMessage(draft.getDraftId(), connection, responseData);
        } catch (Exception ex) {
            log.error("Error in application onMessage logic: {}", ex.getMessage());
            webSocketConnection.setAppResponseData(DEFAULT_MESSAGE_PAYLOAD);
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

//        log.info("Response: {}", JSONUtils.toJSONString(responseData));
        return responseData;
    }

    @Override
    public void onClose(WebSocketConnection webSocketConnection, int code, String reason, boolean remote) {
        var connection = new AppConnection(webSocketConnection);
        ConnectionsRegistry.unregister(connection);
        roomRegistry.removeConnection(connection);
        throw new ClientDisconnectException("Client sent CLOSE frame");
    }

    @Override
    public void onError(WebSocketConnection webSocketConnection, Exception exception) {
        log.error("Error handling input stream: ", exception);

    }

    @Override
    public void onPing(WebSocketConnection webSocketConnection, ByteBuffer payload) {
        webSocketConnection.setResponseFrame(ControlFrame.pong());
        ConnectionsRegistry.refresh(webSocketConnection.getNetworkRequestData());
    }

    private DraftAction getLastAction(Draft draft) {
        return draft.getActions().get(draft.getActions().size() - 1);
    }

    private void broadcastMessage(String roomId, AppConnection newConnection, DraftResponseData responseData) {
        var targetRoom = roomRegistry.getRoomById(roomId);
        try {
            targetRoom.addMember(newConnection);
            var connectionsToBroadcast = targetRoom.getConnectionsInRoomToBroadcast(List.of(newConnection));
            broadCastMessage(connectionsToBroadcast, responseData);
        } catch (Exception e) {
            log.error("Error broadcasting message: {}", e);
            targetRoom.remove(newConnection);
        }
    }

    private void broadCastMessage(List<AppConnection> connections, DraftResponseData message) {
        log.info("Broadcasting message to {} members", connections.size());
        for (AppConnection connection : connections) {
            RedisClient.notifyStickyServer(connection.getSenderId(), message);
        }
    }
}
