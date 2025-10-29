package com.freenote.app.server.application.models.enums;

import lombok.Getter;

public enum RequestType {
    CONNECT(1),
    INVALID(-1);

    @Getter
    final int code;

    RequestType(int code) {
        this.code = code;
    }

    public static RequestType fromCode(int code) {
        for (RequestType actionType : RequestType.values()) {
            if (actionType.code == code) {
                return actionType;
            }
        }
        return INVALID;
    }
}
