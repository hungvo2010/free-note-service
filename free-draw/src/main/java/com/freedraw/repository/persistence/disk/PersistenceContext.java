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

    public void persist(Draft draft) {
        var draftPosition = this.searchDraftIx.insert(draft.getDraftId());
        log.info("Persisting draftId: {}, position: {}", draft.getDraftId(), draftPosition);

        var actionsVector = (VariableLengthFieldSearchByOffset<String>) this.searchOffsetMap.get("actions");
        var actionsStartLengthOffsets = (FixedLengthFieldSearchByOffset<Integer[]>) this.searchOffsetMap.get("actions.offsets");

        // Use getOrAppend to handle new drafts automatically
        int currentActionsStart = actionsVector.getSize();
        var startLength = actionsStartLengthOffsets.getOrAppend(draftPosition, new Integer[]{currentActionsStart, 0});
        
        if (startLength == null) {
            log.error("Failed to get or create start/length for draftId: {}, position: {}", draft.getDraftId(), draftPosition);
            return;
        }
        
        var start = startLength[0];
        var length = startLength[1];

        log.info("Start: {}, length: {}", start, length);
        
        // Validate the data
        if (start < 0 || length < 0 || length > 1000000) {
            log.error("Invalid start/length values for draftId: {}, start: {}, length: {}. Reinitializing.", 
                     draft.getDraftId(), start, length);
            start = currentActionsStart;
            length = 0;
        }
        
        int newActionsCount = draft.getActions().size();
        
        if (newActionsCount == length) {
            log.info("No new actions to persist for draftId: {}", draft.getDraftId());
            return;
        }
        
        if (newActionsCount < length) {
            log.warn("Draft has fewer actions ({}) than previously stored ({}). Resetting.", newActionsCount, length);
            start = currentActionsStart;
            length = 0;
        }
        
        // Update the offset with new length
        actionsStartLengthOffsets.put(draftPosition, new int[]{start, newActionsCount});

        // Append only the NEW actions (from 'length' index to end)
        var newActions = draft.getActions().subList(length, newActionsCount);
        log.info("Appending {} new actions for draftId: {}", newActions.size(), draft.getDraftId());
        
        for (var action : newActions.stream().filter(Objects::nonNull).toList()) {
            actionsVector.append(JSONUtils.toJSONString(action));
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

    private Draft buildDraftById(String draftId, int idx) {
        var draft = new Draft(draftId);
        draft.setActions(getDraftActionsStartLength(idx));
        return draft;
    }

    private List<DraftAction> getDraftActionsStartLength(int draftPosition) {
        var startLength = getStartLength((FixedLengthFieldSearchByOffset<Integer[]>) this.searchOffsetMap.get("actions.offsets"), draftPosition);
        var start = startLength[0];
        var length = startLength[1];
        var actionsVector = (VariableLengthFieldSearchByOffset<String>) this.searchOffsetMap.get("actions");
        var result = new ArrayList<DraftAction>();
        for (int j = 0; j < length; j++) {
            var actionData = actionsVector.getData(start + j);
            if (!actionData.isEmpty()) {
                log.info("Action data: {}", actionData);
            }
            result.add(JSONUtils.fromJSON(actionData, DraftAction.class));
        }
        return result;
    }

    private Integer[] getStartLength(FixedLengthFieldSearchByOffset<Integer[]> actionsStartLengthOffsets, int draftPosition) {
        return actionsStartLengthOffsets.getData(draftPosition);

    }

    private void initOrLoadFileOffsets() throws IOException {
        var fileOffsets = FileUtility.findFile("offsets");
        this.searchOffset = new SearchDraftActionsRangeByOffset(fileOffsets.getPath());
    }

    private void initOrLoadFileIndex() throws IOException {
        var draftIdIdx = FileUtility.findFile("draftId.idx");
        this.searchDraftIx = new SearchIxImpl<>(draftIdIdx.getPath(), String.class);
    }

    private void initOrLoadSingleFieldData() throws IOException {
        var fileActionOffsets = FileUtility.findFile("actions.offsets");
        var fileActions = FileUtility.findFile("actions");
        this.searchOffsetMap.put("actions.offsets", new FixedLengthFieldSearchByOffset<>(fileActionOffsets.getPath(), Integer[].class));
        this.searchOffsetMap.put("actions", new VariableLengthFieldSearchByOffset<>(fileActions.getPath(), String.class));
    }

}

