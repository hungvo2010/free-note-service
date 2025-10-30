package com.freenote.app.server.application.models.core;

import com.freenote.app.server.application.models.enums.ActionType;
import com.freenote.app.server.application.models.request.DraftContent;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
public class DraftAction {
    public static final DraftAction INVALID = new DraftAction(ActionType.INVALID);
    @Getter
    private ActionType actionType = ActionType.INIT;
    private Map<String, Object> data = new HashMap<>();

    public DraftAction(ActionType actionType) {
        this.actionType = actionType;
    }

    public DraftAction(DraftContent requestContent) {
        this.actionType = ActionType.valueOf(requestContent.getAttribute("type"));
        this.data.putAll(requestContent.getAttributes());
    }

    public void addData(String key, Object value) {
        data.put(key, value);
    }

    public Map<String, Object> getData() {
        return Collections.unmodifiableMap(this.data);
    }
}
