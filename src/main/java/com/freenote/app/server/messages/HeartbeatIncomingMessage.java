package com.freenote.app.server.messages;

import com.freenote.app.server.core.connection.WebSocketConnection;
import com.freenote.app.server.dto.HeartbeatMsg;
import com.freenote.app.server.handler.endpoint.AbstractEndpointHandler;
import com.freenote.app.server.model.enums.MsgType;
import com.freenote.app.server.model.ws.CommonResponseObject;

public class HeartbeatIncomingMessage implements IncomingMessage {
    private final HeartbeatMsg requestMessage;

    public HeartbeatIncomingMessage(HeartbeatMsg requestMessage) {
        this.requestMessage = requestMessage;
    }

    @Override
    public void handle(AbstractEndpointHandler handler, WebSocketConnection connection) {
        connection.setResponseObject(new CommonResponseObject<>(HeartbeatMsg.builder()
                .msgType(MsgType.PONG)
                .pingAt(requestMessage.getPingAt())
                .receivedPingAt(System.currentTimeMillis())
                .pongAt(System.currentTimeMillis())
                .build()));
    }
}
