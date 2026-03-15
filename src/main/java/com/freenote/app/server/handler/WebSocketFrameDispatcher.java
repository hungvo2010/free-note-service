package com.freenote.app.server.handler;

import com.freenote.app.server.core.WebSocketConnection;
import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.util.FrameUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * Dispatcher for WebSocket frames based on their Opcode.
 * This class uses a Table-Driven approach to eliminate switch-case statements.
 */
public class WebSocketFrameDispatcher {
    private static final Logger log = LogManager.getLogger(WebSocketFrameDispatcher.class);

    @FunctionalInterface
    public interface FrameHandlerAction {
        void accept(WebSocketHandler handler, WebSocketConnection connection, WebSocketFrame frame) throws IOException;
    }

    private static final Map<FrameType, FrameHandlerAction> HANDLERS = new EnumMap<>(FrameType.class);

    static {
        HANDLERS.put(FrameType.PING, (handler, conn, frame) -> handler.onPing(conn, null));
        HANDLERS.put(FrameType.PONG, (handler, conn, frame) -> handler.onPong(conn, null));
        HANDLERS.put(FrameType.TEXT, (handler, conn, frame) -> handler.onMessage(conn, getContent(frame)));
        HANDLERS.put(FrameType.BINARY, (handler, conn, frame) -> handler.onMessage(conn, getBuffer(frame)));
        HANDLERS.put(FrameType.CLOSE, (handler, conn, frame) -> handler.onClose(conn, 0, "", true));
        HANDLERS.put(FrameType.CONTINUATION, (handler, conn, frame) -> handler.onContinue(conn, null));
    }

    /**
     * Dispatches the frame to the appropriate method on the handler.
     *
     * @param handler    The WebSocketHandler instance to call methods on.
     * @param connection The current WebSocketConnection.
     * @param frame      The WebSocketFrame to process.
     * @throws IOException If an I/O error occurs during processing.
     */
    public static void dispatch(WebSocketHandler handler, WebSocketConnection connection, WebSocketFrame frame) throws IOException {
        FrameType type = FrameType.fromHexValue(frame.getOpcode());
        HANDLERS.getOrDefault(type, WebSocketFrameDispatcher::handleUnknown).accept(handler, connection, frame);
    }

    private static void handleUnknown(WebSocketHandler handler, WebSocketConnection connection, WebSocketFrame frame) {
        log.error("Unknown frame type: {}", frame.getOpcode());
    }

    public static String getContent(WebSocketFrame frame) {
        byte[] payload = frame.isMasked() ? FrameUtil.maskPayload(frame.getPayloadData(), frame.getMaskingKey()) : frame.getPayloadData();
        return new String(payload, StandardCharsets.UTF_8);
    }

    public static ByteBuffer getBuffer(WebSocketFrame frame) {
        return ByteBuffer.wrap(FrameUtil.maskPayload(frame.getPayloadData(), frame.getMaskingKey()));
    }
}
