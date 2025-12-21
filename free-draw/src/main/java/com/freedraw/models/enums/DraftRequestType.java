package com.freedraw.models.enums;

import lombok.Getter;

public enum DraftRequestType {
    CONNECT(1),
    DATA(2),
    INVALID(-1);

    @Getter
    final int code;

    DraftRequestType(int code) {
        this.code = code;
    }

    public static DraftRequestType fromCode(int code) {
        for (DraftRequestType actionType : DraftRequestType.values()) {
            if (actionType.code == code) {
                return actionType;
            }
        }
        return INVALID;
    }
}
