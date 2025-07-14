package com.freenote.app.server.frames.base;

import com.freenote.app.server.util.FrameUtil;

public class DataFrame extends WebSocketFrame {
    @Override
    protected void parsePayloadLength(byte[] bytes) {
        masked = ((bytes[1] & 0x80) >> 7) == 1;
        payloadLength = FrameUtil.parsePayloadLength(bytes);
    }
}
