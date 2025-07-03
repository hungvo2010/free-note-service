package com.freenote.app.server.frames;

import java.io.IOException;
import java.io.ObjectOutput;

public class PongFrame extends BaseFrame {
    public PongFrame() {
        super(FrameType.PONG.getHexValue());
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        var outputBytes = FrameFactory.createServerFrame(new byte[0], FrameType.PONG);
        out.write(outputBytes);
    }
}
