package com.freedraw.repository.persistence.disk.service.impl.generic;

import com.freedraw.repository.persistence.disk.service.SearchFieldByOffset;
import com.freedraw.repository.persistence.disk.type.DataType;
import com.freedraw.repository.persistence.disk.type.DataTypeRegistry;
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
            long position = offset * itemSize;
            // Bounds check
            if (position >= this.fileReader.length()) {
                log.warn("Offset {} exceeds file length, returning null", offset);
                return null;
            }
            this.fileReader.seek(position);
            byte[] byteValues = new byte[(int) itemSize];
            this.fileReader.read(byteValues);
            return dataType.fromBytes(byteValues);
        } catch (IOException e) {
            log.error("Error reading data at offset: {}, exception: {}", offset, e.getMessage());
            return null;
        }
    }

    /**
     * Gets data at offset, or appends default value if position doesn't exist.
     * More robust for handling new entries with automatic gap filling.
     */
    public T getOrAppend(long offset, T defaultValue) {
        try {
            long position = offset * itemSize;
            long fileLength = this.fileReader.length();
            
            // If position exists, read it
            if (position < fileLength) {
                this.fileReader.seek(position);
                byte[] byteValues = new byte[(int) itemSize];
                this.fileReader.read(byteValues);
                T existingValue = dataType.fromBytes(byteValues);
                
                // Validate the data for Integer[] type (start/length pairs)
                if (existingValue instanceof Integer[]) {
                    Integer[] values = (Integer[]) existingValue;
                    if (values.length >= 2) {
                        // Check if values are corrupted (negative or unreasonably large)
                        if (values[0] < 0 || values[1] < 0 || values[1] > 1000000) {
                            log.warn("Corrupted data at offset {}: [{}, {}]. Overwriting with default.", 
                                    offset, values[0], values[1]);
                            // Overwrite with default value
                            this.fileReader.seek(position);
                            var byteBuffer = ByteBuffer.allocate((int) this.itemSize);
                            byteBuffer.put(dataType.toBytes(defaultValue));
                            this.fileReader.write(byteBuffer.array());
                            return defaultValue;
                        }
                    }
                }
                
                return existingValue;
            }
            
            // Position doesn't exist - need to append
            long currentItems = fileLength / itemSize;
            
            // Fill gaps if offset is beyond current end
            while (currentItems <= offset) {
                this.fileReader.seek(this.fileReader.length());
                var byteBuffer = ByteBuffer.allocate((int) this.itemSize);
                byteBuffer.put(dataType.toBytes(defaultValue));
                this.fileReader.write(byteBuffer.array());
                currentItems++;
            }
            
            log.info("Appended default value at offset: {}", offset);
            return defaultValue;
            
        } catch (IOException e) {
            log.error("Error in getOrAppend at offset: {}, exception: {}", offset, e.getMessage());
            return null;
        }
    }

    /**
     * Returns the number of items currently stored in the file.
     */
    public int getCount() {
        try {
            long fileLength = this.fileReader.length();
            return (int) (fileLength / itemSize);
        } catch (IOException e) {
            log.error("Error getting count for file: {}, exception: {}", this.path, e.getMessage());
            return 0;
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

    public void put(long startPosition, int[] newStartLength) {
        var startByte = this.itemSize * startPosition;
        try {
            // Bounds check - ensure position exists
            if (startByte >= this.fileReader.length()) {
                log.error("Cannot put at position {}: exceeds file length. Use append() for new entries.", startPosition);
                return;
            }
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
