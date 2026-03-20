package com.freedraw.service;

import com.freedraw.dto.DraftRequestData;
import com.freedraw.entities.Draft;
import com.freedraw.entities.DraftAction;
import com.freedraw.exception.DraftNotFoundException;
import com.freedraw.repository.DraftRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DraftService {
    private static final Logger log = LogManager.getLogger(DraftService.class);
    private final DraftRepository draftRepository;

    // Standard Clean Code Constructor Injection [7]
    public DraftService(DraftRepository draftRepository) {
        this.draftRepository = draftRepository;
    }

    public Draft handleDraftRequest(DraftRequestData data) {
        try {
            return processRequest(data);
        } catch (Exception e) {
            handleFailure(e);
            throw e;
        }
    }

    private Draft processRequest(DraftRequestData data) {
        // Apply Guard Clauses for flatter logic [11]
        if (data.isNewUpdate()) {
            return createAndSaveNewDraft(data);
        }

        validateExistingRequest(data);
        
        Draft draft = getDraftOrThrow(data.getDraftId());
        applyActionToDraft(draft, data);
        
        draftRepository.save(draft);
        return draft;
    }

    private void validateExistingRequest(DraftRequestData data) {
        if (data.getDraftId() == null || data.getDraftId().isEmpty()) {
            throw new IllegalArgumentException("Draft ID is required for existing draft request");
        }
    }

    private void applyActionToDraft(Draft draft, DraftRequestData data) {
        // Logic for merging shapes now resides IN Draft or DraftAction [3]
        DraftAction action = (data.isConnect()) ? 
            draft.generateMergedAction() : // Move Method: Draft merges its own shapes
            new DraftAction(data.getContent());

        draft.addAction(action);
    }

    private Draft createAndSaveNewDraft(DraftRequestData data) {
        Draft draft = Draft.createNew(data.getContent()); // Static Factory Method [14]
        draftRepository.save(draft);
        return draft;
    }

    private Draft getDraftOrThrow(String id) {
        Draft draft = draftRepository.getDraftById(id);
        if (draft == null) {
            throw new DraftNotFoundException("Draft ID: " + id);
        }
        return draft;
    }

    // Single responsibility error handling [12]
    private void handleFailure(Exception e) {
        log.error("Failed to handle draft request", e);
    }
}
