package com.freenote.app.server.handler.impl;


import com.freenote.app.server.connections.WebSocketConnection;
import com.freenote.app.server.exceptions.ClientDisconnectException;
import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.frames.factory.ClientFrameFactory;
import com.freenote.app.server.frames.factory.FrameFactory;
import com.freenote.app.server.frames.factory.ServerFrameFactory;
import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.handler.WebSocketHandler;
import com.freenote.app.server.http.HttpUpgradeRequest;
import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.util.FrameUtil;
import com.freenote.app.server.util.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static com.freenote.app.server.util.IOUtils.getRawBytes;

public class CommonHandlerImpl implements URIHandler, WebSocketHandler {
    private static final Logger log = LogManager.getLogger(CommonHandlerImpl.class);
    private static final ServerFrameFactory serverFrameFactory = new ServerFrameFactory();
    private static final ClientFrameFactory clientFrameFactory = new ClientFrameFactory();

    @Override
    public boolean handle(InputWrapper inputWrapper, OutputStream outputStream) {
        try {
            var inputStream = inputWrapper.getInputStream();
            if (inputStream.available() == 0) {
                return false;
            }
            log.info("EchoHandlerImpl: Received actual data");
            byte[] actualData = getRawBytes(inputStream);

            log.info("Raw bytes length: {}", actualData.length);
            log.info("First 10 bytes: {}", Arrays.toString(Arrays.copyOf(actualData, Math.min(10, actualData.length))));

            // Show as unsigned values
            for (int i = 0; i < Math.min(10, actualData.length); i++) {
                log.info("Byte[{}]: signed={}, unsigned={}, hex=0x{}",
                        i, actualData[i], actualData[i] & 0xFF, Integer.toHexString(actualData[i] & 0xFF));
            }
            WebSocketFrame frame = clientFrameFactory.createFrameFromBytes(actualData);

            log.info("FIN: {}", frame.isFin());
            log.info("Opcode: {} - {}", frame.getOpcode(), FrameType.fromHexValue(frame.getOpcode()));
            log.info("Masked: {}", frame.isMasked());
            log.info("Payload Length: {}", frame.getPayloadLength());
            log.info("Masking Key: {}", frame.getMaskingKey());
            WebSocketConnection webSocketConnection = WebSocketConnection.builder()
                    .inputStream(inputStream)
                    .outputStream(outputStream)
                    .build();

            byte[] payload = frame.isMasked() ? FrameUtil.maskPayload(frame.getPayloadData(), frame.getMaskingKey()) : frame.getPayloadData();
            String content = new String(payload, StandardCharsets.UTF_8);

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

            IOUtils.writeOutPut(outputStream, FrameFactory.CLIENT.createBinaryFrame(webSocketConnection.getResponse().toString().getBytes(StandardCharsets.UTF_8)));
            return true;
        } catch (IOException e) {
            log.error("Error handling input stream", e);
            return false;
        }
    }

    @Override
    public boolean continuationHandler(List<WebSocketFrame> clientFrame, InputWrapper inputWrapper, OutputStream outputStream) {
        return false;
    }

    @Override
    public void onMessage(WebSocketConnection webSocketConnection, String message) {

    }

    @Override
    public void onMessage(WebSocketConnection webSocketConnection, ByteBuffer message) {

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
    public void onError(WebSocketConnection webSocketConnection, Exception throwable) {

    }

    @Override
    public void onPing(WebSocketConnection webSocketConnection, ByteBuffer payload) {

    }

    @Override
    public void onPong(WebSocketConnection webSocketConnection, ByteBuffer payload) {

    }

    @Override
    public void onContinue(WebSocketConnection webSocketConnection, ByteBuffer payload) {

    }
}
