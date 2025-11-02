package com.freenote.app.server.application.models.enums;

import lombok.Getter;

@Getter
public enum ActionType {
    INIT(0),
    INVALID(-1),
    UPDATE(1),
    NOOP(2);
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
