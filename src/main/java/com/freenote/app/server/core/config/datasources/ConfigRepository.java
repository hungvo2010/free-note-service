package com.freenote.app.server.core.config.datasources;

import java.util.*;
import java.util.stream.Collectors;

public class ConfigRepository {
    private List<ConfigSource> configSources = new ArrayList<>();
    private Map<String, String> configMap;

    public void load() {
        this.configSources.add(new FileSource("application.properties"));
        this.configSources = sortedDataSources();
        this.configMap = loadAllDataSources();
    }

    public String getActiveProfile() {
        return configMap.get("freenote.profiles.active");
    }

    public void loadSource(ConfigSource configSource) {
        this.configSources.add(configSource);
        this.configSources = sortedDataSources();
        this.configMap = loadAllDataSources();
    }

    private Map<String, String> loadAllDataSources() {
        var allKeysMap = new HashMap<String, String>();
        for (var configSource : configSources) {
            for (var key : configSource.getAllKeys()) {
                allKeysMap.computeIfAbsent(key, k -> configSource.get(key));
            }
        }
        return allKeysMap;
    }

    private List<ConfigSource> sortedDataSources() {
        return this.configSources.stream()
                .sorted(Comparator.comparingInt(ConfigSource::priority).reversed())
                .collect(Collectors.toList());
    }

    public String get(String configKey) {
        return this.configMap.get(configKey);
    }

    public FileSource resolve(String activeProfile) {
        return new FileSource("application-" + activeProfile + ".properties", 10);
    }
}
