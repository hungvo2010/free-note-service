package com.freenote.app.server.application.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freenote.app.server.application.models.request.DraftRequest;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Draft {
    @JsonProperty("draft_id")
    @Getter
    private String draftId;
    private JsonNode jsonNode;

    @JsonIgnore
    private List<DraftAction> actions = new ArrayList<>();

    public Draft() {
        this.draftId = UUID.randomUUID().toString();
    }

    public void addAction(DraftAction action) {
        actions.add(action);
    }

    public void updateNewString(String newDraftContent) {
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

    private DraftAction compareDiffAction(JsonNode jsonNode, JsonNode newDraftJson) {
        return null;
    }

    public DraftAction doRequest(DraftRequest draftRequest) {
        updateNewString(draftRequest.toString());
        return actions.get(actions.size() - 1);
    }

    public static void main(String[] args) throws JsonProcessingException {
        Draft draft = new Draft();
        DraftAction action = new DraftAction();
        draft.addAction(action);
        System.out.println(new ObjectMapper().writeValueAsString(draft));
    }
}
