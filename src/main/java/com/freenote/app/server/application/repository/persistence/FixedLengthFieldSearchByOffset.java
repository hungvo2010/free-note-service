package com.freenote.app.server.application.repository.persistence;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FixedLengthFieldSearchByOffset implements SearchFieldByOffset, Closeable {
    private final RandomAccessFile fileReader;

    public FixedLengthFieldSearchByOffset(String path, int i) throws IOException {
        this.fileReader = new RandomAccessFile(path, "r");
    }

    @Override
    public Object find(int offset) {
        return null;
    }

    @Override
    public void close() throws IOException {

    }
}
