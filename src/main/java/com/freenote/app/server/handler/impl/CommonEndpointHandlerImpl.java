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
import com.freenote.app.server.model.enums.MsgType;
import com.freenote.app.server.model.ws.CommonResponseObject;
import com.freenote.app.server.util.FrameUtil;
import com.freenote.app.server.util.IOUtils;
import com.freenote.app.server.util.JSONUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static com.freenote.app.server.util.IOUtils.getRawBytes;

public abstract class CommonEndpointHandlerImpl implements URIHandler, WebSocketHandler {
    private static final Logger log = LogManager.getLogger(CommonEndpointHandlerImpl.class);

    @Override
    public boolean handle(InputWrapper inputWrapper, OutputStream outputStream) {
        try {
            var inputStream = inputWrapper.getInputStream();
            if (!isAvailable(inputWrapper.getSocket())) {
                return false;
            }
            byte[] actualData = getRawBytes(inputStream);
            WebSocketFrame frame = FrameFactory.CLIENT.createFrameFromBytes(actualData);

            log.debug("FIN: {}", frame.isFin());
            log.debug("Opcode: {} - {}", frame.getOpcode(), FrameType.fromHexValue(frame.getOpcode()));
            log.debug("Masked: {}", frame.isMasked());
            log.debug("Payload Length: {}", frame.getPayloadLength());
            log.debug("Masking Key: {}", frame.getMaskingKey());
            WebSocketConnection webSocketConnection = WebSocketConnection.builder()
                    .inputStream(inputStream)
                    .outputStream(outputStream)
                    .build();

            byte[] payload = frame.isMasked() ? FrameUtil.maskPayload(frame.getPayloadData(), frame.getMaskingKey()) : frame.getPayloadData();
            String content = new String(payload, StandardCharsets.UTF_8);
            log.debug("Payload: {}", content);

            switch (FrameType.fromHexValue(frame.getOpcode())) {
                case PING:
                    onPing(webSocketConnection, null);
                    break;
                case PONG:
                    onPong(webSocketConnection, null);
                    break;
                case TEXT:
                    onMessage(webSocketConnection, content);
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

    private boolean isAvailable(Socket socket) throws IOException {
        if (socket instanceof SSLSocket) {
            return socket.getInputStream().read() > 0;
        }
        return socket.getInputStream().available() > 0;
    }

    private void sendResponse(WebSocketConnection webSocketConnection) throws IOException {
        if (!Objects.isNull(webSocketConnection.getResponseFrame())) {
            IOUtils.writeOutPut(webSocketConnection.getOutputStream(), webSocketConnection.getResponseFrame());
        } else if (!Objects.isNull(webSocketConnection.getResponseObject())) {
            IOUtils.writeOutPut(
                    webSocketConnection.getOutputStream(),
                    FrameFactory.SERVER.createTextFrame(
                            JSONUtils.toJSONString(webSocketConnection.getResponseObject().getResponseData()
                            )));
        }

    }

    @Override
    public boolean continuationHandler(List<WebSocketFrame> clientFrame, InputWrapper inputWrapper, OutputStream outputStream) {
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

    public abstract void onControl(WebSocketConnection webSocketConnection, ByteBuffer payload);

    @Override
    public void onPong(WebSocketConnection webSocketConnection, ByteBuffer payload) {
        onControl(webSocketConnection, payload);
    }

    @Override
    public void onContinue(WebSocketConnection webSocketConnection, ByteBuffer payload) {
    }
}
