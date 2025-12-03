package com.freenote.app.server.application.factory;

import com.freenote.app.server.application.models.common.MessagePayload;
import com.freenote.app.server.frames.base.WebSocketFrame;

public interface ApplicationFrameFactory {
    ApplicationFrameFactory SERVER = new ServerApplicationFrameFactory();

    WebSocketFrame createApplicationFrame(MessagePayload payload);
}
