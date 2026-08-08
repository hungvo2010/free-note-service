package com.freenote.app.server.core.config;

import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@UtilityClass
public class AppConfig {
    private static final Logger log = LogManager.getLogger(AppConfig.class);
    private static final String CONFIG_FILE = "application.properties";
    private static final Properties properties = new Properties();

    static {
        loadProperties();
    }

    private static void loadProperties() {
        try (InputStream input = AppConfig.class.getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                properties.load(input);
                log.info("Application properties loaded successfully from {}", CONFIG_FILE);
            } else {
                log.warn("{} not found, using default configuration", CONFIG_FILE);
            }
        } catch (IOException e) {
            log.error("Error loading {}: {}", CONFIG_FILE, e.getMessage());
        }
    }

    public static String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value.trim());
    }
}
