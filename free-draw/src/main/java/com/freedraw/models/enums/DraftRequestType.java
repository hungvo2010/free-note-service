package com.freedraw.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum DraftRequestType {
    CONNECT(1),
    ADD(2),
    UPDATE(3),
    REMOVE(4),
    NOOP(5),
    INVALID(-1);

    final int code;

    DraftRequestType(int code) {
        this.code = code;
    }

    @JsonCreator
    public static DraftRequestType fromCode(int code) {
        for (DraftRequestType actionType : DraftRequestType.values()) {
            if (actionType.code == code) {
                return actionType;
            }
        }
        return INVALID;
    }

    @JsonValue
    public int getCode() {
        return code;
    }
}
