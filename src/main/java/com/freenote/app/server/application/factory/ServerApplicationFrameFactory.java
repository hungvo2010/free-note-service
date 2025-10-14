package com.freenote.app.server.application.factory;

import com.freenote.app.server.application.models.common.MessagePayload;
import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.frames.factory.FrameFactory;
import com.freenote.app.server.frames.factory.ServerFrameFactory;

public class ServerApplicationFrameFactory implements ApplicationFrameFactory {
    private final FrameFactory serverFactory = new ServerFrameFactory();

    @Override
    public WebSocketFrame createApplicationFrame(MessagePayload payload) {
        var binaryPayload = payload.toBytes();
        return serverFactory.createBinaryFrame(binaryPayload);
    }
}
