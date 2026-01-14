package com.freedraw.utils;

import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.frames.factory.FrameFactory;
import com.freenote.app.server.util.JSONUtils;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FrameUtils {

    public static WebSocketFrame createApplicationFrame(Object payload) {
        var textPayload = JSONUtils.toJSONString(payload);
        return FrameFactory.SERVER.createTextFrame(textPayload);
    }
}
