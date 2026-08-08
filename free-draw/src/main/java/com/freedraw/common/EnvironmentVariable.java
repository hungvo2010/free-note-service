package com.freedraw.common;

import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

@UtilityClass
public class EnvironmentVariable {
    private static final Logger log = LogManager.getLogger(EnvironmentVariable.class);

    public static String getTargetDirectory() {
        try {
            String targetDir = System.getenv("FREENOTE_TARGET_DIRECTORY");
            String platform = System.getProperty("os.name").toLowerCase();
            if (platform.contains("win")) {
                if (targetDir == null || targetDir.isEmpty()) {
                    targetDir = "D:\\tempdata";
                }
                Files.createDirectories(Path.of(targetDir));
                return targetDir;
            }
            if (targetDir == null || targetDir.isEmpty()) {
                targetDir = "/tmp/freenote_data";
            }
            Files.createDirectories(Path.of(targetDir));
            return targetDir;
        }
        catch (Exception e) {
            log.info("Failed to get target directory from environment variable: {}", e.getMessage());
            return "/tmp/";
        }
    }
}
