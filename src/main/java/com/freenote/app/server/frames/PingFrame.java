package com.freenote.app.server.frames;

import com.freenote.app.server.frames.base.ControlFrame;
import com.freenote.app.server.frames.base.FrameTypeWithBehavior;

public class PingFrame extends ControlFrame {
    public PingFrame() {
        super(FrameTypeWithBehavior.PING.getOpcode());
    }
}
