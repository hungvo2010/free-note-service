package com.freedraw.repository;

import com.freedraw.entities.Draft;

public interface DraftRepository {
    Draft getDraftById(String draftId);

    void save(Draft draft);
}
