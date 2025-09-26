package com.freenote.app.server.application;

import com.freenote.app.server.application.models.MessagePayload;
import com.freenote.app.server.frames.base.WebSocketFrame;

public interface ApplicationFrameFactory {
    public WebSocketFrame createApplicationFrame(MessagePayload payload);
}
