package com.freenote.app.server.application.repository.persistence.disk.service.impl;

import com.freenote.app.server.application.repository.persistence.disk.FileUtility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

public class SearchDraftIdByIndex implements AutoCloseable {
    private static final Logger log = LogManager.getLogger(SearchDraftIdByIndex.class);
    private final RandomAccessFile fileReader;
    private final Map<String, Long> draftIdIndexMap = new HashMap<>();

    public SearchDraftIdByIndex(String filePath) throws IOException {
        this.fileReader = new RandomAccessFile(filePath, "rw");
        long fileSize = this.fileReader.length();
        long itemSize = getItemSize();
        for (int i = 0; i < fileSize / itemSize; i++) {
            this.fileReader.seek(i * itemSize);
            var byteValues = new byte[(int) itemSize];
            this.fileReader.read(byteValues);
            this.setDraftIdByIndex(new String(byteValues).trim(), i);
        }
    }

    private void setDraftIdByIndex(String draftId, int index) {
        draftIdIndexMap.put(draftId, (long) index);
    }

    private static long getItemSize() {
        return 16;
    } // default UUID size = 16

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
        log.info("Inserting draftId: {}, index: {}", draftId, newIndex);
        FileUtility.writeAt(this.fileReader, draftId.getBytes(), newIndex * getItemSize(), getItemSize());
        return newIndex;
    }

    @Override
    public void close() throws Exception {
        this.fileReader.close();
    }
}
