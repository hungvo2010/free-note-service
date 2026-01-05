package com.freedraw.repository;

import com.freedraw.entities.Draft;

public interface DraftRepository {
    Draft getDraftById(String draftId);

    Draft createNew();

    void save(Draft draft);
}
