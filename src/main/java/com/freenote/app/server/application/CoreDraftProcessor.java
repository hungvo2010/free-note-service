package com.freenote.app.server.application;

import com.freenote.app.server.application.models.common.MessagePayload;
import com.freenote.app.server.application.models.core.Draft;
import com.freenote.app.server.application.models.core.DraftAction;
import com.freenote.app.server.application.models.enums.ActionType;
import com.freenote.app.server.application.models.request.DraftRequest;
import com.freenote.app.server.application.repository.DraftRepository;
import com.freenote.app.server.application.repository.InMemDraftRepositoryImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

public class CoreDraftProcessor {
    private static final Logger log = LogManager.getLogger(CoreDraftProcessor.class);
    private final DraftRepository draftRepository = new InMemDraftRepositoryImpl();

    public MessagePayload processDraft(DraftRequest draftRequest) {
        var draftId = draftRequest.getDraftId();

        if (Objects.isNull(draftId)) {
            log.info("Received new draft request: {}", draftRequest);
            var newDraft = createDraft(draftRequest);
            draftRepository.save(newDraft);
            return new MessagePayload(newDraft);
        }

        var draft = draftRepository.getDraftById(draftId);
        if (Objects.isNull(draft)) {
            throw new IllegalArgumentException("Draft with ID " + draftId + " not found.");
        }

        var result = draft.doRequest(draftRequest);
        return new MessagePayload(result);
    }

    private Draft createDraft(DraftRequest draftRequest) {
        var newDraft = draftRepository.addNewDraft();
        var draftAction = new DraftAction(ActionType.INIT);
        draftAction.addData("draftId", newDraft.getDraftId());
        draftAction.addData("content", draftRequest.getContent());
        newDraft.addAction(draftAction);
        return newDraft;
    }
}
