package com.freenote.app.server.application.models.request;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DraftContent {
    private HashMap<String, Object> data = new HashMap<>();

    public String getAttribute(String key) {
        return (String) data.get(key);
    }

    public Map<String, ?> getAttributes() {
        return Collections.unmodifiableMap(this.data);
    }
}
