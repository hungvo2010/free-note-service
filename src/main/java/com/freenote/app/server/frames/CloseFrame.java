package com.freenote.app.server.frames;

import com.freenote.app.server.frames.base.ControlFrame;

public class CloseFrame extends ControlFrame {
    public CloseFrame() {
        super(FrameType.CLOSE.getOpCode());
    }
}
