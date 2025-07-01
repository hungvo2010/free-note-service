package com.freenote.app.server.frames;

import java.io.IOException;
import java.io.ObjectOutput;

public class PingFrame extends BaseFrame {
    private final FrameType opCode = FrameType.PING;

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        var outputBytes = FrameFactory.createServerFrame(new byte[0], opCode);
        out.write(outputBytes);
    }
}
