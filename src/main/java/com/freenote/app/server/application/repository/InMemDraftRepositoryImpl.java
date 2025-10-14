package com.freenote.app.server.application.repository;

import com.freenote.app.server.application.models.core.Draft;
import com.freenote.app.server.application.repository.persistence.PersistenceContext;

import java.util.ArrayList;
import java.util.List;

public class InMemDraftRepositoryImpl implements DraftRepository {
    private final List<Draft> allDrafts = new ArrayList<>();
    private final PersistenceContext persistenceContext;

    public InMemDraftRepositoryImpl() {
        this.persistenceContext = new PersistenceContext();
        this.persistenceContext.initData();
    }

    @Override
    public Draft getDraftById(String draftId) {
        for (Draft draft : allDrafts) {
            if (draft.getDraftId().equals(draftId)) {
                return draft;
            }
        }
        return null;
    }

    @Override
    public Draft addNewDraft() {
        var newDraft = new Draft();
        allDrafts.add(newDraft);
        return newDraft;
    }

    @Override
    public void save(Draft draft) {
        this.persistenceContext.persist(draft);
    }
}
