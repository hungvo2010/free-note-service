package com.freedraw.factory;

import com.freedraw.models.common.AppMessage;
import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.frames.factory.FrameFactory;
import com.freenote.app.server.frames.factory.ServerFrameFactory;
import com.freenote.app.server.util.JSONUtils;

public class ServerApplicationFrameFactory implements ApplicationFrameFactory {
    private final FrameFactory serverFactory = new ServerFrameFactory();

    @Override
    public WebSocketFrame createApplicationFrame(AppMessage payload) {
        var binaryPayload = JSONUtils.toJSONBytes(payload);
        return serverFactory.createBinaryFrame(binaryPayload);
    }
}
