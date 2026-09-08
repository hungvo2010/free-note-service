package com.freenote.app.server.core.config.datasources;

import com.freenote.app.server.core.config.AppConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

public class FileSource implements ConfigSource {
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
        try (InputStream input = AppConfig.class.getClassLoader()
                .getResourceAsStream(configFile)) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load properties", e);
        }
    }
}
