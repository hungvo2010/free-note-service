package com.freenote.app.server.application.repository.persistence.disk;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.List;

public class FixedLengthFieldSearchByOffset implements SearchFieldByOffset, Closeable {
    private static final Logger log = LogManager.getLogger(FixedLengthFieldSearchByOffset.class);
    private final RandomAccessFile fileReader;
    private final long itemSize;
    private String path;

    public FixedLengthFieldSearchByOffset(String path) throws IOException {
        this.fileReader = new RandomAccessFile(path, "r");
        this.itemSize = this.getItemSize(path);
        this.path = path;
    }

    private long getItemSize(String filePath) {
        return Integer.parseInt(filePath.substring(filePath.lastIndexOf(",") + 1, filePath.lastIndexOf(".")));
    }

    @Override
    public Object find(long offset) {
        try {
            this.fileReader.seek(offset * itemSize);
            byte[] byteValues = new byte[(int) itemSize];
            var byteBuffer = ByteBuffer.wrap(byteValues);
            this.fileReader.read(byteValues);
            return byteBuffer.get();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void close() throws IOException {
        fileReader.close();
    }


    public void append(List<?> newData) {
        try {
            var startByte = this.fileReader.length();
            this.fileReader.seek(startByte);
            for (Object data : newData) {
                var byteBuffer = ByteBuffer.allocate((int) this.itemSize);
                byteBuffer.put((byte) data);
                this.fileReader.write(byteBuffer.array());
            }
        } catch (IOException e) {
            log.error("Error appending data to file: {}, exception: {}", this.path, e.getMessage());
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
