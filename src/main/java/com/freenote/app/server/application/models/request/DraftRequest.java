package com.freenote.app.server.application.models.request;

import java.util.HashMap;
import java.util.Map;

public class DraftRequest {
    private String draftId;
    private Map<String, Object> content;

    public String getDraftId() {
        return this.draftId;
    }

    public Map<String, Object> getContent() {
        return new HashMap<>(this.content);
    }
}
