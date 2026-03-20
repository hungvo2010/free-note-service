package com.unleak.sdk;

import java.util.HashMap;
import java.util.Map;

public class CompositeInput {
    private Map<String, Object> inputMap = new HashMap<>();

    public Object getValue(String interestedKey) {
        return this.inputMap.get(interestedKey);
    }

    public void addValue(String key, Object value) {
        this.inputMap.put(key, value);
    }
}
