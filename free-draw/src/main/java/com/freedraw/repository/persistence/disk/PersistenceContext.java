package com.freedraw.repository.persistence.disk;

import com.freedraw.entities.Draft;
import com.freedraw.entities.DraftAction;
import com.freedraw.repository.persistence.disk.service.SearchFieldByOffset;
import com.freedraw.repository.persistence.disk.service.SearchIx;
import com.freedraw.repository.persistence.disk.service.SearchOffset;
import com.freedraw.repository.persistence.disk.service.impl.SearchDraftActionsRangeByOffset;
import com.freedraw.repository.persistence.disk.service.impl.generic.FixedLengthFieldSearchByOffset;
import com.freedraw.repository.persistence.disk.service.impl.generic.SearchIxImpl;
import com.freedraw.repository.persistence.disk.service.impl.generic.VariableLengthFieldSearchByOffset;
import com.freenote.app.server.util.JSONUtils;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

@Getter
public class PersistenceContext {
    private static final Logger log = LogManager.getLogger(PersistenceContext.class);
    private SearchIx<String> searchDraftIx;
    private SearchIx<Integer> searchActionType;
    private SearchOffset searchOffset;
    private final Map<String, SearchFieldByOffset> searchOffsetMap = new HashMap<>();

    public void initData() {
        try {
            initOrLoadFileIndex();
            initOrLoadFileOffsets();
            initOrLoadSingleFieldData();
        } catch (IOException e) {
            log.error("Error initializing PersistenceContext: {}", e.getMessage());
        }
    }

    private void initOrLoadFileOffsets() throws IOException {
        var fileOffsets = FileUtility.findFile("offsets");
        this.searchOffset = new SearchDraftActionsRangeByOffset(fileOffsets.getPath());
    }

    private void initOrLoadFileIndex() throws IOException {
        var draftIdIdx = FileUtility.findFile("draftId.idx");
        var actionTypeIdx = FileUtility.findFile("draft.actionType.idx");
        this.searchDraftIx = new SearchIxImpl<>(draftIdIdx.getPath(), String.class);
        this.searchActionType = new SearchIxImpl<>(actionTypeIdx.getPath(), Integer.class);
    }

    private void initOrLoadSingleFieldData() throws IOException {
        var fileActionOffsets = FileUtility.findFile("actions.offsets");
        this.searchOffsetMap.put("actions.offsets", new FixedLengthFieldSearchByOffset<>(fileActionOffsets.getPath(), Integer[].class));
        this.searchOffsetMap.put("actions", new VariableLengthFieldSearchByOffset<>(fileActionOffsets.getPath(), String.class));
    }

    public void persist(Draft draft) {
        var draftPosition = this.searchDraftIx.insert(draft.getDraftId());
        log.info("Persisting draftId: {}, position: {}", draft.getDraftId(), draftPosition);

        var actionsVector = (VariableLengthFieldSearchByOffset<String>) this.searchOffsetMap.get("actions");
        var actionsOffsets = (FixedLengthFieldSearchByOffset<Integer[]>) this.searchOffsetMap.get("actions.offsets");

        var startLength = getStartLength(draftPosition);
        var start = startLength[0];
        var length = startLength[1];

        log.info("Start: {}, length: {}", start, length);
        if (draft.getActions().size() == length) {
            log.info("No new actions to persist for draftId: {}", draft.getDraftId());
            return;
        }
        var newLength = draft.getActions().size();
        actionsOffsets.update(draftPosition, new int[]{start, newLength});

        for (var action : draft.getActions().stream().filter(Objects::nonNull).toList()) {
            actionsVector.append(JSONUtils.toJSONString(action));
            var idx = this.searchActionType.insert(action.getActionType().getCode());
            log.info("Persisting actionType: {}, idx: {}", action.getActionType().getCode(), idx);
        }
    }

    public List<Draft> getAllDrafts() {
        var allDraftIds = this.searchDraftIx.getAll();
        var result = new ArrayList<Draft>();
        for (int idx = 0; idx < allDraftIds.size(); idx++) {
            var draftId = allDraftIds.get(idx);
            log.info("DraftId: {}", draftId);
            result.add(buildDraftById(draftId, idx));
        }
        return result;
    }

    private Draft buildDraftById(String draftId, int i) {
        var draft = new Draft(draftId);
        draft.setActions(getDraftActionsStartLength(i));
        return draft;
    }

    private List<DraftAction> getDraftActionsStartLength(int draftPosition) {
        var startLength = getStartLength(draftPosition);
        var start = startLength[0];
        var length = startLength[1];
        var actionsVector = (VariableLengthFieldSearchByOffset<String>) this.searchOffsetMap.get("actions");
        var result = new ArrayList<DraftAction>();
        for (int j = 0; j < length; j++) {
            var actionData = actionsVector.getData(start + j);
            if (!actionData.isEmpty()){
                log.info("Action data: {}", actionData);
            }
            result.add(JSONUtils.fromJSON(actionData, DraftAction.class));
        }
        return result;
    }

    private Integer[] getStartLength(int draftPosition) {
        var actionsOffsets = (FixedLengthFieldSearchByOffset<Integer[]>) this.searchOffsetMap.get("actions.offsets");
        return actionsOffsets.getData(draftPosition);

    }

}

