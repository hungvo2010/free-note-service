package com.freenote.app.server.application.models.enums;

import lombok.Getter;

public enum ActionType {
    UPDATE(1),
    INIT(0),
    INVALID(-1);

    @Getter
    final int code;

    ActionType(int code) {
        this.code = code;
    }

    public static ActionType fromCode(int code) {
        for (ActionType actionType : ActionType.values()) {
            if (actionType.code == code) {
                return actionType;
            }
        }
        return INVALID;
    }
}
