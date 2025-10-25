package com.freenote.app.server.application.repository.persistence.disk.service.impl.generic;

import com.freenote.app.server.application.repository.persistence.disk.service.SearchFieldByOffset;
import com.freenote.app.server.application.repository.persistence.disk.type.DataType;
import com.freenote.app.server.application.repository.persistence.disk.type.DataTypeRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.List;

public class FixedLengthFieldSearchByOffset<T> implements SearchFieldByOffset<T>, Closeable {
    private static final Logger log = LogManager.getLogger(FixedLengthFieldSearchByOffset.class);
    private final RandomAccessFile fileReader;
    private final DataType<T> dataType;
    private final long itemSize;
    private final String path;

    public FixedLengthFieldSearchByOffset(String path, Class<T> fieldType) throws IOException {
        this.fileReader = new RandomAccessFile(path, "rw");
        this.path = path;
        this.dataType = DataTypeRegistry.get(fieldType);
        this.itemSize = this.dataType.getSize();
    }

    private long getItemSize(String filePath) {
        return Integer.parseInt(filePath.substring(filePath.lastIndexOf(",") + 1, filePath.lastIndexOf(".")));
    }

    @Override
    public T getData(long offset) {
        try {
            this.fileReader.seek(offset * itemSize);
            byte[] byteValues = new byte[(int) itemSize];
            this.fileReader.read(byteValues);
            return dataType.fromBytes(byteValues);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public int append(T item) {
        try {
            var startByte = this.fileReader.length();
            this.fileReader.seek(startByte);
            var byteBuffer = ByteBuffer.allocate((int) this.itemSize);
            byteBuffer.put(dataType.toBytes(item));
            this.fileReader.write(byteBuffer.array());
            return (int) (startByte / this.itemSize);
        } catch (IOException e) {
            log.error("Error appending data to file: {}, exception: {}", this.path, e.getMessage());
            return -1;
        }
    }


    @Override
    public void close() throws IOException {
        fileReader.close();
    }


    public void append(List<T> newData) {
        for (var data : newData) {
            this.append(data);
        }
    }

    public void update(long draftPosition, int[] newStartLength) {
        var startByte = this.itemSize * draftPosition;
        try {
            this.fileReader.seek(startByte);
            var byteBuffer = ByteBuffer.allocate((int) this.itemSize);
            byteBuffer.putInt(newStartLength[0]);
            byteBuffer.putInt(newStartLength[1]);
            this.fileReader.write(byteBuffer.array());
        } catch (IOException e) {
            log.error("Error updating data in file: {}, exception: {}", this.path, e.getMessage());
        }
    }
}
