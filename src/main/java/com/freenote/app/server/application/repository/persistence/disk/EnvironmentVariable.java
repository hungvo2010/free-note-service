package com.freenote.app.server.application.repository.persistence.disk;

import lombok.experimental.UtilityClass;

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
        return targetDir;
    }
}
