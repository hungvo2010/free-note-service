package com.freenote.app.server.application.models.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freenote.app.server.application.models.request.DraftRequest;
import lombok.Getter;
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
    @JsonIgnore
    private final List<DraftAction> actions = new ArrayList<>();

    public Draft() {
        this.draftId = UUID.randomUUID().toString();
    }

    public void addAction(DraftAction action) {
        actions.add(action);
    }

    public DraftAction doRequest(DraftRequest draftRequest) {
        var requestContent = draftRequest.getContent();
        return new DraftAction(requestContent);
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
