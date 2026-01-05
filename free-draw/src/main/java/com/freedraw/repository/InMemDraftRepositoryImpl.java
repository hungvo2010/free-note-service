package com.freedraw.repository;

import com.freedraw.entities.Draft;
import com.freedraw.repository.persistence.disk.PersistenceContext;

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
    public Draft createNew() {
        var newDraft = new Draft();
        allDrafts.add(newDraft);
        return newDraft;
    }

    @Override
    public void save(Draft draft) {
        this.persistenceContext.persist(draft);
    }
}
