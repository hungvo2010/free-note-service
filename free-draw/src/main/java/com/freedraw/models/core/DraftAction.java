package com.freedraw.models.core;

import com.freedraw.models.enums.ActionType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
public class DraftAction {
    @Getter
    private ActionType actionType = ActionType.INIT;
    private final Map<String, Object> data = new HashMap<>();

    public DraftAction(ActionType actionType) {
        this.actionType = actionType;
    }

    public DraftAction(DraftContent requestContent) {
        this.actionType = ActionType.fromCode(Integer.parseInt(requestContent.getAttribute("type").toString()));
        this.data.putAll(requestContent.getAttributes());
    }

    public void addData(String key, Object value) {
        data.put(key, value);
    }

    public Map<String, Object> getData() {
        return Collections.unmodifiableMap(this.data);
    }
}
