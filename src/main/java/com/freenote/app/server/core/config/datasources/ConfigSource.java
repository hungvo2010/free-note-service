package com.freenote.app.server.core.config.datasources;

import java.util.Set;

public interface ConfigSource {
    static ConfigSource resolve(String activeProfile) {
        return null;
    }

    String get(String configKey);
    int priority();

    Set<String> getAllKeys();
}
