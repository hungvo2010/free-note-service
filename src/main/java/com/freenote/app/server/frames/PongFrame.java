package com.freenote.app.server.frames;

import com.freenote.app.server.frames.base.ControlFrame;

public class PongFrame extends ControlFrame {
    public PongFrame() {
        super(FrameType.PONG.getOpCode());
    }
}
