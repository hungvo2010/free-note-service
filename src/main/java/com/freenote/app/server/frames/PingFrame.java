package com.freenote.app.server.frames;

public class PingFrame extends BaseFrame {
    public PingFrame() {
        super(FrameType.PING.getHexValue());
    }

//    @Override
//    public void writeExternal(ObjectOutput out) throws IOException {
//        var outputBytes = FrameFactory.createServerFrame(new byte[0], FrameType.PING);
//        out.write(outputBytes);
//    }
}
