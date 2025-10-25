package com.freenote.app.server.application.models.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DraftContent {
    private final HashMap<String, Object> data = new HashMap<>();

    public String getAttribute(String key) {
        return (String) data.get(key);
    }

    public Map<String, ?> getAttributes() {
        return Collections.unmodifiableMap(this.data);
    }

    @JsonAnySetter
    public void addAttribute(String key, Object value) {
        data.put(key, value);
    }
}

