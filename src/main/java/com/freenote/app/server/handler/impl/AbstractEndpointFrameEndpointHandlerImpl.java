package com.freenote.app.server.handler.impl;


import com.freenote.app.server.core.WebSocketConnection;
import com.freenote.app.server.dto.HeartbeatMsg;
import com.freenote.app.server.exceptions.ClientDisconnectException;
import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.frames.factory.FrameFactory;
import com.freenote.app.server.handler.URIEndpointHandler;
import com.freenote.app.server.handler.frames.WebSocketFrameDispatcher;
import com.freenote.app.server.handler.frames.WebSocketFrameHandler;
import com.freenote.app.server.model.http.HttpUpgradeRequest;
import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.model.OutputWrapper;
import com.freenote.app.server.model.enums.MsgType;
import com.freenote.app.server.model.ws.CommonResponseObject;
import com.freenote.app.server.util.FullFrameParser;
import com.freenote.app.server.util.JSONUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public abstract class AbstractEndpointFrameEndpointHandlerImpl implements URIEndpointHandler, WebSocketFrameHandler {
    private static final Logger log = LogManager.getLogger(AbstractEndpointFrameEndpointHandlerImpl.class);
    private final FullFrameParser frameParser = new FullFrameParser();

    @Override
    public boolean handle(InputWrapper inputWrapper, OutputWrapper outputWrapper) {
        try {
            serveConnection(inputWrapper, outputWrapper);
            return true;
        } catch (IOException e) {
            log.error("Error handling input stream", e);
            return false;
        }
    }

    private void serveConnection(InputWrapper inputWrapper, OutputWrapper outputWrapper) throws IOException {
        byte[] actualData = getRawBytes(inputWrapper);
        WebSocketFrame wsFrame = FrameFactory.CLIENT.createFrameFromBytes(actualData);

        logFrameProperties(wsFrame);
        WebSocketConnection webSocketConnection = buildWebSocketConnection(inputWrapper, outputWrapper);

        WebSocketFrameDispatcher.dispatch(this, webSocketConnection, wsFrame);
        sendResponse(webSocketConnection);
    }

    private WebSocketConnection buildWebSocketConnection(InputWrapper inputWrapper, OutputWrapper outputWrapper) {
        return WebSocketConnection.builder()
                .inputWrapper(inputWrapper)
                .outputWrapper(outputWrapper)
                .socketChannel(inputWrapper.getSocketChannel())
                .build();
    }

    private void logFrameProperties(WebSocketFrame frame) {
        log.debug("FIN: {}", frame.isFin());
        log.debug("Opcode: {} - {}", frame.getOpcode(), FrameType.fromHexValue(frame.getOpcode()));
        log.debug("Masked: {}", frame.isMasked());
        log.debug("Payload Length: {}", frame.getPayloadLength());
        log.debug("Masking Key: {}", frame.getMaskingKey());
        log.debug("Payload: {}", WebSocketFrameDispatcher.getContent(frame));
    }

    protected byte[] getRawBytes(InputWrapper inputWrapper) throws IOException {
        return frameParser.getRawBytes(inputWrapper.getInputStream());
    }

    protected void sendResponse(WebSocketConnection webSocketConnection) throws IOException {
        webSocketConnection.sendCurrentResponse();
    }

    @Override
    public boolean continuationHandler(List<WebSocketFrame> clientFrame, InputWrapper inputWrapper, OutputWrapper outputWrapper) {
        return false;
    }

    @Override
    public void onMessage(WebSocketConnection webSocketConnection, String message) {
        var requestMessage = JSONUtils.fromJSON(message, HeartbeatMsg.class);
        if (isAppPing(requestMessage)) {
            log.info("Received PING message at {}, sending PONG response", requestMessage.getPingAt());
            webSocketConnection.setResponseObject(buildAppPong(requestMessage));
            return;
        }
        onData(webSocketConnection, message);
    }

    private CommonResponseObject<HeartbeatMsg> buildAppPong(HeartbeatMsg requestMessage) {
        return new CommonResponseObject<>(HeartbeatMsg.builder()
                .msgType(MsgType.PONG)
                .pingAt(requestMessage.getPingAt())
                .receivedPingAt(System.currentTimeMillis())
                .pongAt(System.currentTimeMillis())
                .build());
    }

    private boolean isAppPing(HeartbeatMsg requestMessage) {
        return requestMessage != null && requestMessage.getMsgType() == MsgType.PING;
    }

    public void onData(WebSocketConnection webSocketConnection, String message) {
        webSocketConnection.sendText(message);
    }

    @Override
    public void onMessage(WebSocketConnection webSocketConnection, ByteBuffer message) {
        onBinaryMessage(webSocketConnection, message);
    }

    protected void onBinaryMessage(WebSocketConnection webSocketConnection, ByteBuffer message) {
        log.info("Received binary message of length {}, sender: {}", message.remaining(), webSocketConnection.getRemoteAddress());
    }


    @Override
    public void onOpen(WebSocketConnection webSocketConnection, HttpUpgradeRequest handshake) {

    }

    @Override
    public void onClose(WebSocketConnection webSocketConnection, int code, String reason, boolean remote) {
        log.warn("Received CLOSE frame. No further processing.");
        throw new ClientDisconnectException("Client sent CLOSE frame");
    }

    @Override
    public void onError(WebSocketConnection webSocketConnection, Exception exception) {
        log.error("Received ERROR frame", exception);
    }

    @Override
    public void onPing(WebSocketConnection webSocketConnection, ByteBuffer payload) {
        onControl(webSocketConnection, payload);
    }

    public void onControl(WebSocketConnection webSocketConnection, ByteBuffer payload) {
    }

    @Override
    public void onPong(WebSocketConnection webSocketConnection, ByteBuffer payload) {
        onControl(webSocketConnection, payload);
    }

    @Override
    public void onContinue(WebSocketConnection webSocketConnection, ByteBuffer payload) {
    }
}
