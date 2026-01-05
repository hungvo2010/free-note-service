package com.freedraw.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.freedraw.dto.DraftRequest;
import com.freedraw.models.enums.ActionType;
import com.freedraw.models.enums.DraftRequestType;
import com.freenote.app.server.util.JSONUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Draft {
    @Getter
    private String draftId;
    private String draftName;

    @Getter
    @Setter
    @JsonIgnore
    private List<DraftAction> actions = new ArrayList<>();

    public Draft() {
        this.draftId = UUID.randomUUID().toString();
    }

    public Draft(String draftId, String draftName) {
        this.draftId = draftId;
        this.draftName = draftName;
    }

    public Draft(String draftId) {
        this.draftId = draftId;
    }

    public void addAction(DraftAction action) {
        actions.add(action);
    }

    public DraftAction doRequest(DraftRequest draftRequest) {
        if (draftRequest.getDraftRequestType() == DraftRequestType.CONNECT) {
            return connectAction(draftRequest);
        }
        return new DraftAction(draftRequest.getContent());
    }

    private DraftAction connectAction(DraftRequest draftRequest) {
        var requestDraftId = draftRequest.getDraftId();
        return new DraftAction(ActionType.NOOP);
    }

    public static void main(String[] args) throws JsonProcessingException {
        Draft draft = new Draft();
        DraftAction action = new DraftAction();
        draft.addAction(action);
        Logger log = LogManager.getLogger(Draft.class);
        log.info(JSONUtils.toJSONString(draft));
    }

}
