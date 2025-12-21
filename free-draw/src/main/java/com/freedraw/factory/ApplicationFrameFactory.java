package com.freedraw.factory;

import com.freedraw.models.common.MessagePayload;
import com.freenote.app.server.frames.base.WebSocketFrame;

public interface ApplicationFrameFactory {
    ApplicationFrameFactory SERVER = new ServerApplicationFrameFactory();

    WebSocketFrame createApplicationFrame(MessagePayload payload);
}
