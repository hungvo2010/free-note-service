package com.freedraw.responses;

import com.freedraw.models.common.AppMessage;
import com.freenote.app.server.util.JSONUtils;

public class InternalServerError extends AppMessage {
    public InternalServerError(String errorMessage) {
        super(JSONUtils.toJSONString(new ServerResponse(502, errorMessage)));
    }
}
