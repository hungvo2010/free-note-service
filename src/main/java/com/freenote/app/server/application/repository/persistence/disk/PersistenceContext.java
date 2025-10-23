package com.freenote.app.server.application.repository.persistence.disk;

import com.freenote.app.server.application.models.core.Draft;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
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
        var fileOffsets = findFile("offsets");
        this.searchOffset = new SearchDraftActionsByOffset(fileOffsets.getPath());
    }

    private void initOrLoadFileIndex() throws IOException {
        var fileIndex = findFile("draftId.idx");
        this.searchDraftByIndex = SearchDraftIdByIndex.fromFile(fileIndex.getPath());
    }

    private void initOrLoadSingleFieldData() throws IOException {
        var fileDraftActionType = findFile("draftAction.type");
        var fileActionOffsets = findFile("actions.offsets");
        this.searchOffsetMap.put("draftAction.type", new FixedLengthFieldSearchByOffset(fileDraftActionType.getPath()));
        this.searchOffsetMap.put("actions.offsets", new FixedLengthFieldSearchByOffset(fileActionOffsets.getPath()));
    }

    private File findFile(String field) {
        var fileIndex = findExistingFile(field);
        if (fileIndex == null) {
            fileIndex = createFile(field);
        }
        return fileIndex;
    }

    private File findExistingFile(String field) {
        var directory = getTargetDirectory();
        return getFile(directory + "/" + field + ".dat");
    }

    private File createFile(String fieldOffsets) {
        var path = Paths.get(getTargetDirectory(), fieldOffsets, ".dat");
        var file = path.toFile();
        if (!file.exists()) {
            try {
                var result = file.createNewFile();
                if (!result) {
                    return null;
                }
            } catch (IOException e) {
                return null;
            }
        }
        return file;
    }

    private String getTargetDirectory() {
        return EnvironmentVariable.getTargetDirectory();
    }

    private File getFile(String filePath) {
        try {
            return new File(filePath);
        } catch (Exception e) {
            return null;
        }
    }

    public void persist(Draft draft) {
        var draftPosition = this.searchDraftByIndex.getIndexByDraftId(draft.getDraftId());
        var actionsOffset = (FixedLengthFieldSearchByOffset) this.searchOffsetMap.get("actionsOffset");
        var actionsVector = (FixedLengthFieldSearchByOffset) this.searchOffsetMap.get("actions");
        var startLength = (int[]) actionsOffset.find(draftPosition);
        var start = startLength[0];
        var length = startLength[1];
        if (draft.getActions().size() == length) {
            log.info("No new actions to persist for draftId: {}", draft.getDraftId());
            return;
        }
        var newLength = draft.getActions().size();
        actionsOffset.update(draftPosition, new int[]{start, newLength});
        actionsVector.append(draft.getActions());
    }
}
