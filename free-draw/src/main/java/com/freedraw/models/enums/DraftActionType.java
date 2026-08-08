package com.freedraw.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum DraftActionType {
    INIT(0),
    INVALID(-1),
    UPDATE(1),
    NOOP(2);
    final int code;

    DraftActionType(int code) {
        this.code = code;
    }

    @JsonCreator
    public static DraftActionType fromCode(int code) {
        for (DraftActionType actionType : DraftActionType.values()) {
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
