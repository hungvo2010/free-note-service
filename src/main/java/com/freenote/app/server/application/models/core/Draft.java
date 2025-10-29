package com.freenote.app.server.application.models.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freenote.app.server.application.models.enums.ActionType;
import com.freenote.app.server.application.models.enums.RequestType;
import com.freenote.app.server.application.models.request.DraftRequest;
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
    private JsonNode jsonNode;

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
        if (draftRequest.getRequestType() == RequestType.CONNECT) {
            return connectAction(draftRequest);
        }
        var requestContent = draftRequest.getContent();
        var returnAction =  new DraftAction(requestContent);
        RoomManager.getInstance().broadcastToRoom(draftRequest.getDraftId(), returnAction);
        return returnAction;

    }

    private DraftAction connectAction(DraftRequest draftRequest) {
        var requestDraftId = draftRequest.getDraftId();
        RoomManager.getInstance().addConnection(requestDraftId, draftRequest);
        return new DraftAction(ActionType.NOOP);
    }

    private DraftAction compareDiffAction(JsonNode jsonNode, JsonNode newDraftJson) {
        return null;
    }

    private void updateNewString(String newDraftContent) {
        var newDraftJson = createJSON(newDraftContent);
        this.jsonNode = newDraftJson;
        var newInferredAction = compareDiffAction(this.jsonNode, newDraftJson);
        addAction(newInferredAction);
    }

    private JsonNode createJSON(String newDraftContent) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(newDraftContent);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public static void main(String[] args) throws JsonProcessingException {
        Draft draft = new Draft();
        DraftAction action = new DraftAction();
        draft.addAction(action);
        Logger log = LogManager.getLogger(Draft.class);
        log.info(new ObjectMapper().writeValueAsString(draft));
    }

}
