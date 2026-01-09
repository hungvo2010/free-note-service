package com.freenote.app.server.common;

import lombok.experimental.UtilityClass;

import java.net.URI;
import java.nio.file.Path;

@UtilityClass
public class EnvironmentVariable {
    public static String getTargetDirectory() {
        String targetDir = System.getenv("FREENOTE_TARGET_DIRECTORY");
        String platform = System.getProperty("os.name").toLowerCase();
        if (platform.contains("win")) {
            if (targetDir == null || targetDir.isEmpty()) {
                targetDir = "D:\\tempdata";
            }
            return targetDir;
        }
        if (targetDir == null || targetDir.isEmpty()) {
            targetDir = "/tmp/freenote_data";
        }
        if (!Path.of(URI.create(targetDir)).toFile().exists()) {
            var creationResult = Path.of(URI.create(targetDir)).toFile().mkdirs();
            if (!creationResult) {
                throw new RuntimeException("Failed to create target directory at: " + targetDir);
            }
        }
        return targetDir;
    }
}
