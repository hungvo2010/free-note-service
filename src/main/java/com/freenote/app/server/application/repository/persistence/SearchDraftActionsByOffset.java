package com.freenote.app.server.application.repository.persistence;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;

public class SearchDraftActionsByOffset implements Closeable, SearchOffset {
    private final RandomAccessFile fileReader;

    public SearchDraftActionsByOffset(String filePath) throws IOException {
        this.fileReader = new RandomAccessFile(filePath, "r");
    }

    public int[] searchByOffset(int offset) {
        int[] result = new int[2];
        try {
            fileReader.seek(offset * getItemSize());
            result[0] = fileReader.readInt();
            result[1] = fileReader.readInt();
            return result;
        } catch (IOException e) {
            return new int[]{-1, -1};
        }
    }

    private int getItemSize() {
        return 0;
    }

    @Override
    public void close() throws IOException {
        this.fileReader.close();
    }
}
