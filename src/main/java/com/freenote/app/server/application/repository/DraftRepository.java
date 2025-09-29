package com.freenote.app.server.application.repository;

import com.freenote.app.server.application.models.Draft;

public interface DraftRepository {
    public Draft getDraftById(String draftId);

    Draft addNewDraft();
}
