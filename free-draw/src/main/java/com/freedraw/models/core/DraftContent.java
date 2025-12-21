package com.freedraw.models.core;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DraftContent {
    private final HashMap<String, Object> data = new HashMap<>();

    public Object getAttribute(String key) {
        return data.get(key);
    }

    public Map<String, ?> getAttributes() {
        return Collections.unmodifiableMap(this.data);
    }

    @JsonAnySetter
    public void addAttribute(String key, Object value) {
        data.put(key, value);
    }
}

