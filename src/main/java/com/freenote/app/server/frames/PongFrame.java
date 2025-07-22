package com.freenote.app.server.frames;

import com.freenote.app.server.frames.base.ControlFrame;
import com.freenote.app.server.frames.base.FrameTypeWithBehavior;

public class PongFrame extends ControlFrame {
    public PongFrame() {
        super(FrameTypeWithBehavior.PONG.getOpcode());
    }
}
