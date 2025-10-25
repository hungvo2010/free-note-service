package com.freenote.app.server.application.repository.persistence.disk;

import com.freenote.app.server.application.models.core.Draft;
import com.freenote.app.server.application.repository.persistence.disk.service.SearchFieldByOffset;
import com.freenote.app.server.application.repository.persistence.disk.service.SearchOffset;
import com.freenote.app.server.application.repository.persistence.disk.service.impl.SearchDraftActionsByOffset;
import com.freenote.app.server.application.repository.persistence.disk.service.impl.SearchDraftIdByIndex;
import com.freenote.app.server.application.repository.persistence.disk.service.impl.generic.FixedLengthFieldSearchByOffset;
import com.freenote.app.server.application.repository.persistence.disk.service.impl.generic.VariableLengthFieldSearchByOffset;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Getter
public class PersistenceContext {
    private static final Logger log = LogManager.getLogger(PersistenceContext.class);
    private SearchDraftIdByIndex searchDraftByIndex;
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
        this.searchOffset = new SearchDraftActionsByOffset(fileOffsets.getPath());
    }

    private void initOrLoadFileIndex() throws IOException {
        var fileIndex = FileUtility.findFile("draftId.idx");
        this.searchDraftByIndex = new SearchDraftIdByIndex(fileIndex.getPath());
    }

    private void initOrLoadSingleFieldData() throws IOException {
        var fileDraftActionType = FileUtility.findFile("draftAction.type,4");
        var fileActionOffsets = FileUtility.findFile("actions.offsets,8");
        this.searchOffsetMap.put("draftAction.type", new FixedLengthFieldSearchByOffset<>(fileDraftActionType.getPath(), Integer.class));
        this.searchOffsetMap.put("actions.offsets", new FixedLengthFieldSearchByOffset<>(fileActionOffsets.getPath(), Integer[].class));
        this.searchOffsetMap.put("actions", new VariableLengthFieldSearchByOffset<>(fileActionOffsets.getPath(), String.class));
    }

    public void persist(Draft draft) {
        var draftPosition = this.searchDraftByIndex.insertDraftId(draft.getDraftId());
        log.info("Persisting draftId: {}, position: {}", draft.getDraftId(), draftPosition);

        var actionsOffsets = (FixedLengthFieldSearchByOffset<Integer[]>) this.searchOffsetMap.get("actions.offsets");
        var actionsVector = (VariableLengthFieldSearchByOffset<String>) this.searchOffsetMap.get("actions");
        var startLength = actionsOffsets.getData(draftPosition);
        var start = startLength[0];
        var length = startLength[1];

        log.info("Start: {}, length: {}", start, length);
        if (draft.getActions().size() == length) {
            log.info("No new actions to persist for draftId: {}", draft.getDraftId());
            return;
        }
        var newLength = draft.getActions().size();
        actionsOffsets.update(draftPosition, new int[]{start, newLength});
        for (var action : draft.getActions()) {
            actionsVector.append(action.toString());
        }
    }
}

