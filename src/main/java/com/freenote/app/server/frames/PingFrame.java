package com.freenote.app.server.frames;

import com.freenote.app.server.frames.base.ControlFrame;

public class PingFrame extends ControlFrame {
    public PingFrame() {
        super(FrameType.PING.getOpCode());
    }
}
