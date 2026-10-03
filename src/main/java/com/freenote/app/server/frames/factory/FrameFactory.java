package com.freenote.app.server.frames.factory;

import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.frames.base.ControlFrame;
import com.freenote.app.server.frames.base.DataFrame;
import com.freenote.app.server.frames.ws.WebSocketFrame;
import com.freenote.app.server.util.JSONUtils;

import java.security.SecureRandom;

public interface FrameFactory {
    WebSocketFrame createTextFrame(String text);

    WebSocketFrame createBinaryFrame(byte[] data);

    WebSocketFrame createPingFrame();

    WebSocketFrame createPongFrame();

    WebSocketFrame createCloseFrame(int code, String reason);

    WebSocketFrame createContinuationFrame(byte[] data);

    WebSocketFrame createFrameFromBytes(byte[] frameBytes);

    WebSocketFrame createNonFinalFrame(short opCode, byte[] data);

    FrameFactory SERVER = new ServerFrameFactory();
    FrameFactory CLIENT = new ClientFrameFactory();

    class ServerFrameFactory implements FrameFactory {
        @Override
        public WebSocketFrame createTextFrame(String text) {
            return new DataFrame(FrameType.TEXT.getOpCode(), text.getBytes());
        }

        public WebSocketFrame createApplicationFrame(Object payload) {
            return createTextFrame(JSONUtils.toJSONString(payload));
        }

        @Override
        public WebSocketFrame createBinaryFrame(byte[] data) {
            return new DataFrame(FrameType.BINARY.getOpCode(), data);
        }

        @Override
        public WebSocketFrame createPingFrame() {
            return ControlFrame.ping();
        }

        @Override
        public WebSocketFrame createPongFrame() {
            return ControlFrame.pong();
        }

        @Override
        public WebSocketFrame createCloseFrame(int code, String reason) {
            return ControlFrame.close();
        }

        @Override
        public WebSocketFrame createContinuationFrame(byte[] data) {
            var dataFrame = new DataFrame(FrameType.CONTINUATION.getOpCode(), data);
            dataFrame.setFin(false);
            return dataFrame;
        }

        @Override
        public WebSocketFrame createFrameFromBytes(byte[] frameBytes) {
            var serverFrame = DataFrame.fromRawFrameBytes(frameBytes);
            serverFrame.setMasked(false);
            return serverFrame;
        }

        @Override
        public WebSocketFrame createNonFinalFrame(short opCode, byte[] data) {
            if (opCode == FrameType.CONTINUATION.getOpCode()) {
                throw new IllegalArgumentException("Continuation frames cannot be non-final frames.");
            }
            var frame = new DataFrame(opCode, data);
            frame.setFin(false);
            return frame;
        }
    }

    class ClientFrameFactory implements FrameFactory {
        private final SecureRandom secureRandom = new SecureRandom();

        @Override
        public WebSocketFrame createTextFrame(String text) {
            var bytes = new byte[4];
            secureRandom.nextBytes(bytes);
            return new DataFrame(FrameType.TEXT.getOpCode(), text.getBytes(), true, bytes);
        }

        @Override
        public WebSocketFrame createBinaryFrame(byte[] data) {
            var bytes = new byte[4];
            secureRandom.nextBytes(bytes);
            return new DataFrame(FrameType.BINARY.getOpCode(), data, true, bytes);
        }

        @Override
        public WebSocketFrame createPingFrame() {
            return ControlFrame.ping();
        }

        @Override
        public WebSocketFrame createPongFrame() {
            return ControlFrame.pong();
        }

        @Override
        public WebSocketFrame createCloseFrame(int code, String reason) {
            return ControlFrame.close();
        }

        @Override
        public WebSocketFrame createContinuationFrame(byte[] data) {
            var bytes = new byte[4];
            secureRandom.nextBytes(bytes);
            var dataFrame = new DataFrame(FrameType.CONTINUATION.getOpCode(), data, true, bytes);
            dataFrame.setFin(false);
            return dataFrame;
        }

        @Override
        public WebSocketFrame createFrameFromBytes(byte[] frameBytes) {
            return DataFrame.fromRawFrameBytes(frameBytes);
        }

        @Override
        public WebSocketFrame createNonFinalFrame(short opCode, byte[] data) {
            if (opCode == FrameType.CONTINUATION.getOpCode()) {
                throw new IllegalArgumentException("Continuation frames cannot be non-final frames.");
            }
            var maskingKey = new byte[4];
            secureRandom.nextBytes(maskingKey);
            var frame = new DataFrame(opCode, data, true, maskingKey);
            frame.setFin(false);
            return frame;
        }
    }
}