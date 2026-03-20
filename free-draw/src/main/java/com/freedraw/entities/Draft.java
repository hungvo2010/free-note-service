package com.freedraw.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.freedraw.dto.DraftRequestContent;
import com.freedraw.dto.ShapeData;
import com.freenote.app.server.util.JSONUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class Draft {
    private final String draftId;
    private String draftName;

    @Getter
    @Setter
    @JsonIgnore
    private List<DraftAction> actions = new ArrayList<>();

    public Draft() {
        this.draftId = UUID.randomUUID().toString();
        this.draftName = "Untitled";
    }

    public Draft(String draftId, String draftName) {
        this.draftId = draftId;
        this.draftName = draftName;
    }

    public Draft(String draftId) {
        this.draftId = draftId;
    }

    public static Draft createNew(DraftRequestContent content) {
        Draft draft = new Draft();
        DraftAction draftAction = new DraftAction(content);
        draftAction.putData("draftId", draft.getDraftId());
        draftAction.putData("content", content);
        draft.addAction(draftAction);
        return draft;
    }

    public DraftAction generateMergedAction() {
        var shapeMap = new LinkedHashMap<String, ShapeData>();
        for (var action : actions) {
            for (var shape : action.getShapes()) {
                shapeMap.put(shape.getShapeId(), shape);
            }
        }
        return new DraftAction(new ArrayList<>(shapeMap.values()));
    }

    public void addAction(DraftAction action) {
        actions.add(action);
    }


    public static void main(String[] args) {
        Draft draft = new Draft();
        DraftAction action = new DraftAction();
        draft.addAction(action);
        Logger log = LogManager.getLogger(Draft.class);
        log.info(JSONUtils.toJSONString(draft));
    }

}
