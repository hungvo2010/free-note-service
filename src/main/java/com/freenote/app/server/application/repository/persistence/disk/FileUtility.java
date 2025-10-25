package com.freenote.app.server.application.repository.persistence.disk;

import com.freenote.app.server.common.EnvironmentVariable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Paths;

public class FileUtility {
    private static final File INVALID_FILE = new File("invalid");
    private static final Logger log = LogManager.getLogger(FileUtility.class);

    public static File findFile(String field) {
        var fileIndex = findExistingFile(field);
        if (fileIndex.equals(INVALID_FILE)) {
            fileIndex = createFile(field);
        }
        return fileIndex;
    }

    public static File findExistingFile(String field) {
        var directory = getTargetDirectory();
        return getFile(directory + "/" + field + ".dat");
    }

    public static File createFile(String fieldOffsets) {
        var path = Paths.get(getTargetDirectory(), fieldOffsets, ".dat");
        var file = path.toFile();
        if (!file.exists()) {
            try {
                var result = file.createNewFile();
                if (!result) {
                    return null;
                }
            } catch (IOException e) {
                return null;
            }
        }
        return file;
    }

    public static String getTargetDirectory() {
        return EnvironmentVariable.getTargetDirectory();
    }

    public static File getFile(String filePath) {
        try {
            return new File(filePath);
        } catch (Exception e) {
            return INVALID_FILE;
        }
    }

    public static byte[] readRange(RandomAccessFile fileReader, int start, int length) {
        try {
            fileReader.seek(start);
            byte[] byteValues = new byte[length];
            fileReader.read(byteValues);
            return byteValues;
        } catch (IOException e) {
            return null;
        }
    }

    public static void writeAt(RandomAccessFile fileReader, byte[] bytes, long offset, long bytesLength) {
        try {
            log.info("Writing data to file: {}, start: {}, length: {}", fileReader.toString(), offset, bytesLength);
            fileReader.seek(offset);
            fileReader.write(bytes, 0, (int) bytesLength);
        } catch (IOException e) {
            log.error("Error writing data to file: {}, exception: {}", fileReader.toString(), e.getMessage());
        }
    }
}
