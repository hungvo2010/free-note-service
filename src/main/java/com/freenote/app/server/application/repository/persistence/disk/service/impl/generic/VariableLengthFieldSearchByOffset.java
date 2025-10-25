package com.freenote.app.server.application.repository.persistence.disk.service.impl.generic;

import com.freenote.app.server.application.repository.persistence.disk.FileUtility;
import com.freenote.app.server.application.repository.persistence.disk.service.SearchFieldByOffset;
import com.freenote.app.server.application.repository.persistence.disk.type.DataType;
import com.freenote.app.server.application.repository.persistence.disk.type.DataTypeRegistry;
import com.freenote.app.server.exceptions.DiskReadException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

public class VariableLengthFieldSearchByOffset<T> implements SearchFieldByOffset<T>, Closeable {
    private static final Logger log = LogManager.getLogger(VariableLengthFieldSearchByOffset.class);
    private final RandomAccessFile fileReader;
    private final String path;
    private final DataType<T> dataType;
    private final FixedLengthFieldSearchByOffset<Integer[]> offsetIndexReader;

    public VariableLengthFieldSearchByOffset(String path, Class<T> type) throws IOException {
        this.fileReader = new RandomAccessFile(path, "rw");
        this.path = path;
        this.dataType = DataTypeRegistry.get(type);
        this.offsetIndexReader = new FixedLengthFieldSearchByOffset<>(path + "_index", Integer[].class);
    }
    
    @Override
    public T getData(long offset) {
        try {
            var startLength = getStartLength(offset);
            int start = startLength[0];
            int length = startLength[1];
            var byteData = FileUtility.readRange(this.fileReader, start, length);
            return dataType.fromBytes(byteData);
        } catch (Exception e) {
            throw new DiskReadException("Error reading data at offset: " + offset + " from file: " + this.path, e);
        }
    }

    private int[] getStartLength(long offset) {
        try {
            var startLength = this.offsetIndexReader.getData(offset);
            return new int[]{startLength[0], startLength[1]};
        } catch (Exception e) {
            log.error("Error reading start and length at offset: {} from file: {}, exception: {}", offset, this.path, e.getMessage());
            return new int[]{-1, -1};
        }
    }

    @Override
    public int append(T item) {
        return 0;
    }

    @Override
    public void close() throws IOException {
        fileReader.close();
    }


    public void append(List<T> newData) {
        try {
            for (T data : newData) {
                this.append(data);
            }
        } catch (Exception e) {
            log.error("Error appending data to file: {}, exception: {}", this.path, e.getMessage());
        }
    }
}
