package com.freenote.app.server.application;

import com.freenote.app.server.application.models.ActionType;
import com.freenote.app.server.application.models.DraftAction;
import com.freenote.app.server.application.models.MessagePayload;
import com.freenote.app.server.application.models.request.DraftRequest;
import com.freenote.app.server.application.repository.DraftRepository;
import com.freenote.app.server.application.repository.InMemDraftRepositoryImpl;

import java.util.Objects;

public class CoreDraftProcessor {
    private final DraftRepository draftRepository = new InMemDraftRepositoryImpl();

    public MessagePayload processDraft(DraftRequest draftRequest) {
        var draftId = draftRequest.getDraftId();

        if (Objects.isNull(draftId)) {
            var newDraft = draftRepository.addNewDraft();
            var draftAction = new DraftAction(ActionType.INIT);
            draftAction.addData("draftId", newDraft.getDraftId());
            draftAction.addData("content", draftRequest.getContent());
            return new MessagePayload(new DraftAction(ActionType.INIT));
        }

        var draft = draftRepository.getDraftById(draftId);
        var result = draft.doRequest(draftRequest);
        return new MessagePayload(result);
    }
}
