package com.freedraw.repository;

import com.freedraw.models.core.Draft;

public interface DraftRepository {
    Draft getDraftById(String draftId);

    Draft createNew();

    void save(Draft draft);
}
