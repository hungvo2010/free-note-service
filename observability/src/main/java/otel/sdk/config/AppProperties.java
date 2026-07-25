package otel.sdk.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AppProperties {
    private static final Logger logger = Logger.getLogger(AppProperties.class.getName());
    private static final String CONFIG_FILE = "application.properties";
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = AppProperties.class.getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                PROPERTIES.load(input);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error loading " + CONFIG_FILE + ", using default configuration", e);
        }
    }

    private AppProperties() {
    }

    public static String getOrDefault(String key, String defaultValue) {
        String value = PROPERTIES.getProperty(key);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    public static int getIntOrDefault(String key, int defaultValue) {
        String value = PROPERTIES.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value.trim());
    }
}
