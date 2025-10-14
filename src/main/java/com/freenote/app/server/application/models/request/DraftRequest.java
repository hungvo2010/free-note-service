package com.freenote.app.server.application.models.request;

public class DraftRequest {
    private String draftId;
    private DraftContent content;

    public String getDraftId() {
        return this.draftId;
    }

    public DraftContent getContent() {
        return this.content;
    }
}
