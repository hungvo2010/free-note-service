package com.freenote.app.server.util;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class RuntimeUtils {
    public static void logServerInitialization() {
        log.info("Number of available processors: {}", getAvailableProcessors());
    }

    public static int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }
}
