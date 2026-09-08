package com.freenote.app.server.core.config.datasources;

import java.util.Set;

public class EnvSource implements ConfigSource{
    @Override
    public String get(String configKey) {
        return "";
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public Set<String> getAllKeys() {
        return Set.of();
    }
}
