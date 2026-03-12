package com.freenote.app.server.handler.impl;


import com.freenote.app.server.core.WebSocketConnection;
import com.freenote.app.server.dto.HeartbeatMsg;
import com.freenote.app.server.exceptions.ClientDisconnectException;
import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.frames.factory.FrameFactory;
import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.handler.WebSocketHandler;
import com.freenote.app.server.http.HttpUpgradeRequest;
import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.model.OutputWrapper;
import com.freenote.app.server.model.enums.MsgType;
import com.freenote.app.server.model.ws.CommonResponseObject;
import com.freenote.app.server.util.FrameUtil;
import com.freenote.app.server.util.IOUtils;
import com.freenote.app.server.util.JSONUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

public abstract class CommonEndpointHandlerImpl implements URIHandler, WebSocketHandler {
    private static final Logger log = LogManager.getLogger(CommonEndpointHandlerImpl.class);

    @Override
    public boolean handle(InputWrapper inputWrapper, OutputWrapper outputWrapper) {
        try {
            var inputStream = validateInputStream(inputWrapper);

            byte[] actualData = getRawBytes(inputWrapper);
            WebSocketFrame frame = FrameFactory.CLIENT.createFrameFromBytes(actualData);

            logFrameProperties(frame);
            WebSocketConnection webSocketConnection = buildWebSocketConnection(inputWrapper, outputWrapper);

            switch (FrameType.fromHexValue(frame.getOpcode())) {
                case PING:
                    onPing(webSocketConnection, null);
                    break;
                case PONG:
                    onPong(webSocketConnection, null);
                    break;
                case TEXT:
                    onMessage(webSocketConnection, getContent(frame));
                    break;
                case BINARY:
                    onMessage(webSocketConnection, ByteBuffer.wrap(FrameUtil.maskPayload(frame.getPayloadData(), frame.getMaskingKey())));
                    break;
                case CLOSE:
                    onClose(webSocketConnection, 0, "", true);
                    break;
                case CONTINUATION:
                    onContinue(webSocketConnection, null);
                    break;
                default:
                    log.error("Unknown frame type: {}", frame.getOpcode());
                    break;

            }

            sendResponse(webSocketConnection);
            return true;
        } catch (IOException e) {
            log.error("Error handling input stream", e);
            return false;
        }
    }

    private WebSocketConnection buildWebSocketConnection(InputWrapper inputWrapper, OutputWrapper outputWrapper) {
        return WebSocketConnection.builder()
                .inputWrapper(inputWrapper)
                .outputWrapper(outputWrapper)
                .socketChannel(inputWrapper.getSocketChannel())
                .build();
    }

    private String getContent(WebSocketFrame frame) {
        byte[] payload = frame.isMasked() ? FrameUtil.maskPayload(frame.getPayloadData(), frame.getMaskingKey()) : frame.getPayloadData();
        return new String(payload, StandardCharsets.UTF_8);
    }

    private void logFrameProperties(WebSocketFrame frame) {
        log.debug("FIN: {}", frame.isFin());
        log.debug("Opcode: {} - {}", frame.getOpcode(), FrameType.fromHexValue(frame.getOpcode()));
        log.debug("Masked: {}", frame.isMasked());
        log.debug("Payload Length: {}", frame.getPayloadLength());
        log.debug("Masking Key: {}", frame.getMaskingKey());
        log.debug("Payload: {}", getContent(frame));
    }

    private InputStream validateInputStream(InputWrapper inputWrapper) {
        var inputStream = getInputStream(inputWrapper);
        if (inputStream == null) throw new RuntimeException("Input stream is null");
        return inputStream;
    }

    private InputStream getInputStream(InputWrapper inputWrapper) {
        var inputStream = inputWrapper.getInputStream();
        if (inputStream == null) return null;
        return inputStream;
    }

    protected byte[] getRawBytes(InputWrapper inputWrapper) throws IOException {
        return IOUtils.getRawBytes(inputWrapper.getInputStream());
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
        if (requestMessage != null && requestMessage.getMsgType() == MsgType.PING) {
            log.info("Received PING message at {}, sending PONG response", requestMessage.getPingAt());
            webSocketConnection.setResponseObject(new CommonResponseObject<>(HeartbeatMsg.builder()
                    .msgType(MsgType.PONG)
                    .pingAt(requestMessage.getPingAt())
                    .receivedPingAt(System.currentTimeMillis())
                    .pongAt(System.currentTimeMillis())
                    .build()));
            return;
        }
        onData(webSocketConnection, message);
    }

    public void onData(WebSocketConnection webSocketConnection, String message) {
        webSocketConnection.setResponseFrame(FrameFactory.SERVER.createTextFrame(message));
    }

    @Override
    public void onMessage(WebSocketConnection webSocketConnection, ByteBuffer message) {
        onBinaryMessage(webSocketConnection, message);
    }

    protected void onBinaryMessage(WebSocketConnection webSocketConnection, ByteBuffer message) {
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
