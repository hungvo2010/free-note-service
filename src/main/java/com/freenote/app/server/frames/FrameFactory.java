package com.freenote.app.server.frames;

import com.freenote.app.server.frames.base.FrameBuilder;
import com.freenote.app.server.frames.base.WebSocketFrame;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FrameFactory {
    public static byte[] createServerFrame(byte[] payload, FrameType frameType) {
        byte[] frame = new byte[payload.length + 2];
        frame[0] = frameType.getHexValue();
        frame[1] = (byte) payload.length;
        System.arraycopy(payload, 0, frame, 2, payload.length);
        return frame;
    }

    public static WebSocketFrame createControlFrame(byte[] frame, com.freenote.app.server.frames.base.FrameType frameType) {
        return new WebSocketFrame(frame) {
            @Override
            protected void parsePayloadLength(byte[] bytes) {
                frameType.parseFrame(bytes);
            }
        };
    }

    public static WebSocketFrame createDataFrame(byte[] frame, com.freenote.app.server.frames.base.FrameType frameType) {
        return new WebSocketFrame(frame) {
            @Override
            protected void parsePayloadLength(byte[] bytes) {
                frameType.parseFrame(bytes);
            }
        };
    }

    public static WebSocketFrame createFrame(FrameBuilder frameBuilder, com.freenote.app.server.frames.base.FrameType frameType) {
        return new WebSocketFrame(frameBuilder, frameType) {
            @Override
            protected void parsePayloadLength(byte[] bytes) {

            }
        };
    }
}