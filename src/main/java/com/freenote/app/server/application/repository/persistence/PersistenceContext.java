package com.freenote.app.server.application.repository.persistence;

import com.freenote.app.server.application.models.core.Draft;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class PersistenceContext {
    private SearchDraftIdByIndex searchDraftByIndex;
    private SearchOffset searchOffset;
    private Map<String, SearchFieldByOffset> searchOffsetMap = new HashMap<>();

    public void initData() {
        try {
            initOrLoadFileIndex();
            initOrLoadFileOffsets();
            initOrLoadSingleFieldData();
        } catch (IOException e) {

        }
    }

    private void initOrLoadSingleFieldData() throws IOException {
        var fileDraftActionType = findFile("draftAction.type");
        this.searchOffsetMap.put("draftAction.type", new FixedLengthFieldSearchByOffset(fileDraftActionType.getPath(), 4));
    }

    private void initOrLoadFileOffsets() throws IOException {
        var fileOffsets = findFile("offsets");
        this.searchOffset = new SearchDraftActionsByOffset(fileOffsets.getPath());
    }

    private void initOrLoadFileIndex() throws IOException {
        var fileIndex = findFile("draftId.idx");
        this.searchDraftByIndex = SearchDraftIdByIndex.fromFile(fileIndex.getPath());
    }

    private File findFile(String field) {
        var fileIndex = findIndexFile(field);
        if (fileIndex == null) {
            fileIndex = createIndexFile(field);
        }
        return fileIndex;
    }

    private File createIndexFile(String fieldOffsets) {
        var file = new File(getTargetDirectory() + "/" + fieldOffsets + ".idx");
        if (!file.exists()) {
            try {
                var res = file.createNewFile();
            } catch (IOException e) {
                return null;
            }
        }
        return file;
    }

    private File findIndexFile(String field) {
        var directory = getTargetDirectory();
        return getFile(directory + "/" + field + ".idx");
    }

    private Object getTargetDirectory() {
        return null;
    }

    private File getFile(String filePath) {
        try {
            return new File(filePath);
        } catch (Exception e) {
            return null;
        }
    }

    public void handleMultipleRecords(String field, List<Object> events) {
        var fileData = findFieldData(field);
        if (fileData == null) {
            fileData = createFieldData(field);
        }
        for (Object event : events) {
            addData(fileData, event);
        }
    }

    private File findFieldData(String field) {
        return null;
    }

    public void registerField(String field, String fieldValue) {
        var fileData = findFieldData(field);
        if (fileData == null) {
            fileData = createFieldData(field);
        }
        addData(fileData, fieldValue);
    }

    private void addData(File fileData, Object newData) {

    }

    private File createFieldData(String field) {
        return null;
    }

    public void persist(Draft draft) {
        var draftPosition = this.searchDraftByIndex.getIndexByDraftId(draft.getDraftId());
        var actionsStartLength = this.searchOffset.searchByOffset((int) draftPosition);
        var length = actionsStartLength[1];
        var start = actionsStartLength[0];
        if (length == draft.getActions().size()) {
            return ;
        }

    }
}
