package com.freenote.app.server.application.responses;

import com.freenote.app.server.application.models.common.MessagePayload;
import com.freenote.app.server.util.JSONUtils;

public class InternalServerError extends MessagePayload {
    public InternalServerError(String errorMessage) {
        super(JSONUtils.toJSONString(new ServerResponse(502, errorMessage)));
    }
}
