package com.freedraw;

import com.freedraw.models.enums.MessageType;
import com.freedraw.entities.Draft;
import com.freedraw.entities.DraftAction;
import com.freedraw.dto.DraftRequest;
import com.freedraw.models.enums.ActionType;
import com.freedraw.repository.DraftRepository;
import com.freedraw.repository.InMemDraftRepositoryImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

public class CoreDraftProcessor {
    private static final Logger log = LogManager.getLogger(CoreDraftProcessor.class);
    private final DraftRepository draftRepository = new InMemDraftRepositoryImpl();

    public Draft processDraft(DraftRequest draftRequest, MessageType type) {
        var draftId = draftRequest.getDraftId();

        if (Objects.isNull(draftId)) {
            log.info("Received new draft request: {}", draftRequest);
            return createDraft(draftRequest);
        }

        var draft = draftRepository.getDraftById(draftId);
        if (Objects.isNull(draft)) {
            throw new IllegalArgumentException("Draft with ID " + draftId + " not found.");
        }

        var result = draft.doRequest(draftRequest);
        draftRepository.save(draft);
        return draft;
    }

    private Draft createDraft(DraftRequest draftRequest) {
        var newDraft = draftRepository.createNew();
        var draftAction = new DraftAction(ActionType.INIT);
        draftAction.addData("draftId", newDraft.getDraftId());
        draftAction.addData("content", draftRequest.getContent());
        newDraft.addAction(draftAction);
        draftRepository.save(newDraft);
        return newDraft;
    }
}
