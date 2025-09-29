package com.freenote.app.server.application.repository;

import com.freenote.app.server.application.models.Draft;

import java.util.ArrayList;
import java.util.List;

public class InMemDraftRepositoryImpl implements DraftRepository {
    private final List<Draft> allDrafts = new ArrayList<>();

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
}
