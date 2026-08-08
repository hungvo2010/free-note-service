package com.freedraw.repository.persistence.disk.service.impl;

import com.freedraw.repository.persistence.disk.service.SearchOffset;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;

public class SearchDraftActionsRangeByOffset implements Closeable, SearchOffset {
    private final RandomAccessFile fileReader;
    private final long itemSize;

    public SearchDraftActionsRangeByOffset(String filePath) throws IOException {
        this.fileReader = new RandomAccessFile(filePath, "rw");
        this.itemSize = this.getItemSize(filePath);
    }

    public int[] searchByOffset(int offset) {
        int[] result = new int[2];
        try {
            fileReader.seek(offset * itemSize);
            result[0] = fileReader.readInt();
            result[1] = fileReader.readInt();
            return result;
        } catch (IOException e) {
            return new int[]{-1, -1};
        }
    }

    private int getItemSize(String filePath) {
        // actions,8.N2L
        return 16;
//        return Integer.parseInt(filePath.substring(filePath.lastIndexOf(","), filePath.lastIndexOf(".")));
    }

    @Override
    public void close() throws IOException {
        this.fileReader.close();
    }
}
