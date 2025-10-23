package com.freenote.app.server.application.repository.persistence.disk;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

public class SearchDraftIdByIndex {
    private final Map<String, Long> draftIdIndexMap = new HashMap<>();

    public static SearchDraftIdByIndex fromFile(String filePath) throws IOException {
        var instance = new SearchDraftIdByIndex();
        try (var fileReader = new RandomAccessFile(filePath, "rw")) {
            long fileSize = fileReader.length();
            long itemSize = getItemSize(filePath);
            for (int i = 0; i < fileSize / itemSize; i++) {
                fileReader.seek(i * itemSize);
                var byteValues = new byte[(int) itemSize];
                fileReader.read(byteValues);
                instance.setDraftIdByIndex(new String(byteValues).trim(), i);
            }
        }
        return instance;
    }

    private void setDraftIdByIndex(String draftId, int index) {
        draftIdIndexMap.put(draftId, (long) index);
    }

    private static long getItemSize(String filePath) {
        return 16; // default UUID size = 16
    }

    public long getIndexByDraftId(String draftId) {
        return draftIdIndexMap.getOrDefault(draftId, -1L);
    }

    public long insertDraftId(String draftId) {
        var curIndex = getIndexByDraftId(draftId);
        if (curIndex != -1) {
            return curIndex;
        }
        var newIndex = draftIdIndexMap.size();
        draftIdIndexMap.put(draftId, (long) newIndex);
        return newIndex;
    }
}
