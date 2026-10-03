package com.freenote.app.server.config;

import java.io.IOException;
import java.io.InputStream;
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

    public String getOrDefault(String configKey, String defaultValue) {
        return this.configMap.getOrDefault(configKey, defaultValue);
    }

    public FileSource resolve(String activeProfile) {
        return new FileSource("application-" + activeProfile + ".properties", 10);
    }

    public static interface ConfigSource {

        String get(String configKey);
        int priority();

        Set<String> getAllKeys();
    }

    public static class EnvSource implements ConfigSource {
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

    public static class FileSource implements ConfigSource {
        private final Properties properties = new Properties();
        private final String configFileName;
        private final int priority;

        public FileSource(String filename) {
            this.configFileName = filename;
            this.priority = 0;
            loadProperties(configFileName);
        }

        public FileSource(String filename, int priority) {
            this.configFileName = filename;
            loadProperties(filename);
            this.priority = priority;
        }

        @Override
        public String get(String configKey) {
            return properties.getProperty(configKey);
        }

        @Override
        public int priority() {
            return this.priority;
        }

        @Override
        public Set<String> getAllKeys() {
            return properties.stringPropertyNames();
        }

        private void loadProperties(String configFile) {
            try (InputStream input = FileSource.class.getClassLoader()
                    .getResourceAsStream(configFile)) {
                if (input != null) {
                    properties.load(input);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to load properties", e);
            }
        }
    }
}
