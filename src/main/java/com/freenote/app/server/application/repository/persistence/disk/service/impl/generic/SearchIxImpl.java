package com.freenote.app.server.application.repository.persistence.disk.service.impl.generic;

import com.freenote.app.server.application.repository.persistence.disk.FileUtility;
import com.freenote.app.server.application.repository.persistence.disk.service.SearchIx;
import com.freenote.app.server.application.repository.persistence.disk.type.DataType;
import com.freenote.app.server.application.repository.persistence.disk.type.DataTypeRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchIxImpl<T> implements SearchIx<T>, AutoCloseable {
    private static final Logger log = LogManager.getLogger(SearchIxImpl.class);
    private final RandomAccessFile fileReader;
    private final Map<T, Integer> valueIndexMap = new HashMap<>();
    private final DataType<T> dataType;

    public SearchIxImpl(String filePath, Class<T> fieldType) throws IOException {
        this.fileReader = new RandomAccessFile(filePath, "rw");
        long fileSize = this.fileReader.length();
        this.dataType = DataTypeRegistry.get(fieldType);
        long itemSize = getItemSize();
        for (int i = 0; i < fileSize / itemSize; i++) {
            this.fileReader.seek(i * itemSize);
            var byteValues = new byte[(int) itemSize];
            this.fileReader.read(byteValues);
            this.setDraftIdByIndex(this.dataType.fromBytes(byteValues), i);
        }
    }

    private void setDraftIdByIndex(T value, int index) {
        valueIndexMap.put(value, index);
    }

    private long getItemSize() {
        return this.dataType.getSize();
    } // default UUID size = 16

    @Override
    public T getData(int idx) {
        var allKeys = new ArrayList<>(valueIndexMap.keySet());
        return allKeys.get(idx);
    }

    @Override
    public int insert(T item) {
        if (valueIndexMap.containsKey(item)) {
            return valueIndexMap.get(item);
        }
        var newIndex = valueIndexMap.size();
        valueIndexMap.put(item, newIndex);
        log.info("Inserting value: {}, index: {}", item, newIndex);
        FileUtility.writeAt(this.fileReader, dataType.toBytes((T) item), newIndex * getItemSize(), getItemSize());
        return newIndex;
    }

    @Override
    public List<T> getAll() {
        return new ArrayList<>(valueIndexMap.keySet());
    }

    @Override
    public void close() throws Exception {
        this.fileReader.close();
    }
}
