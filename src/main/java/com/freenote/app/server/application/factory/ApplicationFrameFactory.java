package com.freenote.app.server.application.factory;

import com.freenote.app.server.application.models.common.MessagePayload;
import com.freenote.app.server.frames.base.WebSocketFrame;

public interface ApplicationFrameFactory {
    WebSocketFrame createApplicationFrame(MessagePayload payload);
}
