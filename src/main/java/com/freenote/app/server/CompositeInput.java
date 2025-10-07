package com.freenote.app.server;

import java.util.Map;

public class CompositeInput {
    private Map<String, Object> inputMap;

    public Object getValue(String interestedKey) {
        return this.inputMap.get(interestedKey);
    }
}
