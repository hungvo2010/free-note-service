package com.freenote.app.server.frames;

import com.freenote.app.server.frames.base.ControlFrame;
import com.freenote.app.server.frames.base.DataFrame;
import com.freenote.app.server.frames.base.FrameTypeWithBehavior;
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

    public static WebSocketFrame createControlFrame(byte[] frame, FrameTypeWithBehavior frameTypeWithBehavior) {
        return new ControlFrame(frameTypeWithBehavior.getOpcode());
    }

    public static WebSocketFrame createDataFrame(byte[] frame, FrameTypeWithBehavior frameTypeWithBehavior) {
        return new DataFrame(frameTypeWithBehavior.getOpcode(), frame);
    }
}