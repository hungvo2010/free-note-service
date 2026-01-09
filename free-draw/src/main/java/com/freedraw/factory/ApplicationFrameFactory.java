package com.freedraw.factory;

import com.freedraw.models.common.AppMessage;
import com.freenote.app.server.frames.base.WebSocketFrame;

public interface ApplicationFrameFactory {
    ApplicationFrameFactory SERVER = new ServerApplicationFrameFactory();

    WebSocketFrame createApplicationFrame(AppMessage payload);
}
