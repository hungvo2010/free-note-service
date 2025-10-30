package com.freenote.app.server.application.repository;

import com.freenote.app.server.application.models.core.Draft;

public interface DraftRepository {
    Draft getDraftById(String draftId);

    Draft createNew();

    void save(Draft draft);
}
