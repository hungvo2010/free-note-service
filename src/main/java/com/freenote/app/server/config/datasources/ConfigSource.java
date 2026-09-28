package com.freenote.app.server.config.datasources;

import java.util.Set;

public interface ConfigSource {

    String get(String configKey);
    int priority();

    Set<String> getAllKeys();
}
