package com.freenote.app.server.frames.control;

import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.frames.base.ControlFrame;

public class PongFrame extends ControlFrame {
    public PongFrame() {
        super(FrameType.PONG.getOpCode());
    }
}
