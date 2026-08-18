package com.freedraw.resources;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class PostgresClient {
    private static final Logger log = LogManager.getLogger(PostgresClient.class);
    private static final String SCHEMA_SQL_PATH = "/sql/schema.sql";

    private static volatile HikariDataSource dataSource;

    private PostgresClient() {
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    public static HikariDataSource getDataSource() {
        if (dataSource == null) {
            synchronized (PostgresClient.class) {
                if (dataSource == null) {
                    dataSource = createDataSource();
                    initSchema();
                }
            }
        }
        return dataSource;
    }

    private static HikariDataSource createDataSource() {
        Properties properties = loadProperties();

        String host = properties.getProperty("postgres.host", "localhost");
        String port = properties.getProperty("postgres.port", "5432");
        String database = properties.getProperty("postgres.database", "freedraw");
        String user = properties.getProperty("postgres.user", "postgres");
        String password = properties.getProperty("postgres.password", "postgres");
        int poolSize = Integer.parseInt(properties.getProperty("postgres.connection.pool.size", "10"));
        int minIdle = Integer.parseInt(properties.getProperty("postgres.connection.minimum.idle.size", "2"));
        int timeout = Integer.parseInt(properties.getProperty("postgres.connection.timeout", "3000"));

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%s/%s", host, port, database));
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(timeout);
        config.setPoolName("PostgresDraftStore");

        log.info("Connecting to PostgreSQL at {}:{}/{}", host, port, database);
        HikariDataSource ds = new HikariDataSource(config);
        log.info("PostgreSQL connection pool established");
        return ds;
    }

    private static void initSchema() {
        try (InputStream input = PostgresClient.class.getResourceAsStream(SCHEMA_SQL_PATH)) {
            if (input == null) {
                log.warn("Schema file {} not found, skipping schema init", SCHEMA_SQL_PATH);
                return;
            }
            String schemaSql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute(schemaSql);
                log.info("PostgreSQL schema initialized");
            }
        } catch (IOException | SQLException e) {
            log.error("Failed to initialize PostgreSQL schema: {}", e.getMessage());
        }
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = PostgresClient.class.getClassLoader()
                .getResourceAsStream("postgres.properties")) {
            if (input != null) {
                properties.load(input);
                log.info("PostgreSQL properties loaded successfully");
            } else {
                log.warn("postgres.properties not found, using default configuration");
            }
        } catch (IOException e) {
            log.error("Error loading postgres.properties: {}", e.getMessage());
        }
        return properties;
    }

    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("PostgreSQL connection pool closed");
        }
    }
}
