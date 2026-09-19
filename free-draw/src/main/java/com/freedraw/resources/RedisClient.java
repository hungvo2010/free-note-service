package com.freedraw.resources;

import com.freedraw.common.EnvironmentVariable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class RedisClient {
    private static final Logger log = LogManager.getLogger(RedisClient.class);
    private volatile static RedissonClient redissonClient;
    private static final Properties properties = new Properties();

    static {
        loadProperties();
    }

    private static void loadProperties() {
        try (InputStream input = RedisClient.class.getClassLoader()
                .getResourceAsStream("redis.properties")) {
            if (input != null) {
                properties.load(input);
                log.info("Redis properties loaded successfully");
            } else {
                log.warn("redis.properties not found, using default configuration");
            }
        } catch (IOException e) {
            log.error("Error loading redis.properties: {}", e.getMessage());
        }
    }

    public static RedissonClient getRedissonClient() {
        if (redissonClient == null) {
            synchronized (RedisClient.class) {
                if (redissonClient == null) {
                    redissonClient = createRedissonClient();
                }
            }
        }
        return redissonClient;
    }

    private static RedissonClient createRedissonClient() {
        Config config = new Config();

        String host = properties.getProperty("redis.host", "localhost");
        int port = Integer.parseInt(properties.getProperty("redis.port", "6379"));
        String password = properties.getProperty("redis.password", "");
        int database = Integer.parseInt(properties.getProperty("redis.database", "0"));
        int timeout = Integer.parseInt(properties.getProperty("redis.timeout", "3000"));
        int poolSize = Integer.parseInt(properties.getProperty("redis.connection.pool.size", "64"));
        int minIdleSize = Integer.parseInt(properties.getProperty("redis.connection.minimum.idle.size", "10"));

        String address = String.format("redis://%s:%d", host, port);

        config.useSingleServer()
                .setAddress(address)
                .setDatabase(database)
                .setTimeout(timeout)
                .setConnectionPoolSize(poolSize)
                .setConnectionMinimumIdleSize(minIdleSize);

        if (password != null && !password.isEmpty()) {
            config.useSingleServer().setPassword(password);
        }

        log.info("Connecting to Redis at {}", address);
        RedissonClient client = Redisson.create(config);
        log.info("Redis connection established successfully");

        return client;
    }

    public static void shutdown() {
        if (redissonClient != null && !redissonClient.isShutdown()) {
            redissonClient.shutdown();
            log.info("Redis connection closed");
        }
    }

    public static void notifyStickyServer(String senderId, String payloadJson) {
        if (senderId == null || senderId.isEmpty()) {
            log.warn("Skipping sticky routing: recipient has no senderId");
            return;
        }
        var serverId = EnvironmentVariable.getHostName();
        log.info("SenderId: {}, ServerId: {}", senderId, serverId);
        var mapSenderServer = getRedissonClient().getMapCache(RedisKeys.MAP_SENDER_SERVER);
        mapSenderServer.put(senderId, serverId, 60, TimeUnit.SECONDS);
        var serverTopic = getRedissonClient().getTopic(RedisKeys.SERVER_PREFIX + serverId);
        serverTopic.publish(payloadJson);
    }
}
