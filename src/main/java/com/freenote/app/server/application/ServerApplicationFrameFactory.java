package com.freenote.app.server.application;

import com.freenote.app.server.application.models.MessagePayload;
import com.freenote.app.server.factory.FrameFactory;
import com.freenote.app.server.factory.ServerFrameFactory;
import com.freenote.app.server.frames.base.WebSocketFrame;

public class ServerApplicationFrameFactory implements ApplicationFrameFactory {
    private final FrameFactory serverFactory = new ServerFrameFactory();

    @Override
    public WebSocketFrame createApplicationFrame(MessagePayload payload) {
        var binaryPayload = payload.toBytes();
        return serverFactory.createBinaryFrame(binaryPayload);
    }
}
