package com.freenote.app.server.application.models;

import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
public class DraftAction {
    private ActionType actionType = ActionType.INIT;
    private Map<String, Object> data = new HashMap<>();

    public DraftAction(ActionType actionType) {
        this.actionType = actionType;
    }

    public void addData(String key, Object value) {
        data.put(key, value);
    }
}
