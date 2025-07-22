package com.freenote.app.server.frames;

import com.freenote.app.server.frames.base.ControlFrame;
import com.freenote.app.server.frames.base.DataFrame;
import com.freenote.app.server.frames.base.WebSocketFrame;

public class ServerFrame {
    public static WebSocketFrame createControlFrame(short opcode) {
        return new ControlFrame(opcode);
    }

    public static WebSocketFrame createDataFrame(short opcode) {
        return new DataFrame();
    }
}
