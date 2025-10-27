package com.freenote.app.server.application.repository;

import com.freenote.app.server.application.models.core.Draft;
import com.freenote.app.server.application.repository.persistence.disk.PersistenceContext;

import java.util.List;

public class InMemDraftRepositoryImpl implements DraftRepository {
    private final List<Draft> allDrafts;
    private final PersistenceContext persistenceContext;

    public InMemDraftRepositoryImpl() {
        this.persistenceContext = new PersistenceContext();
        this.persistenceContext.initData();
        allDrafts = this.persistenceContext.getAllDrafts();
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
