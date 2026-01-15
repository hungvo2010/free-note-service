package com.freedraw;

import com.freedraw.dto.DraftRequestData;
import com.freedraw.entities.Draft;
import com.freedraw.entities.DraftAction;
import com.freedraw.exception.DraftNotFoundException;
import com.freedraw.models.enums.ActionType;
import com.freedraw.models.enums.DraftRequestType;
import com.freedraw.repository.DraftRepository;
import com.freedraw.repository.InMemDraftRepositoryImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

public class DraftService {
    private static final Logger log = LogManager.getLogger(DraftService.class);
    private final DraftRepository draftRepository = new InMemDraftRepositoryImpl();

    public Draft handleDraftRequest(DraftRequestData draftRequestData, DraftRequestType type) {
        var draftId = draftRequestData.getDraftId();

        if (Objects.isNull(draftId)) {
            log.info("Received new draft request: {}", draftRequestData);
            return createDraft(draftRequestData);
        }

        var draft = draftRepository.getDraftById(draftId);
        if (Objects.isNull(draft)) {
            throw new DraftNotFoundException("Draft with ID " + draftId + " not found.");
        }

        var draftAction = doRequest(draftRequestData);
        if (draftAction.getActionType() == ActionType.UPDATE) {
            draft.addAction(draftAction);
            draftRepository.save(draft);
        }
        return draft;
    }

    private Draft createDraft(DraftRequestData draftRequestData) {
        var newDraft = new Draft();
        var draftAction = new DraftAction(ActionType.INIT);
        draftAction.addData("draftId", newDraft.getDraftId());
        draftAction.addData("content", draftRequestData.getContent());
        newDraft.addAction(draftAction);
        draftRepository.save(newDraft);
        return newDraft;
    }


    private DraftAction connectAction(DraftRequestData draftRequestData) {
        var requestDraftId = draftRequestData.getDraftId();
        return new DraftAction(ActionType.NOOP);
    }

    private DraftAction doRequest(DraftRequestData draftRequestData) {
        if (draftRequestData.getDraftRequestType() == DraftRequestType.CONNECT) {
            return connectAction(draftRequestData);
        }
        return new DraftAction(draftRequestData.getContent());
    }
}
