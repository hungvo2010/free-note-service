package com.freedraw.resources;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class RedisClient {
    private volatile static RedissonClient redissonClient;

    public static RedissonClient getRedissonClient() {
        if (redissonClient == null) {
            synchronized (RedisClient.class) {
                if (redissonClient == null) {
                    Config config = new Config();
                    config.useSingleServer().setAddress("redis://157.66.219.174:6379");

                    RedissonClient redisson = Redisson.create(config);
                    redissonClient = redisson;
                }
            }
        }
        return redissonClient;
    }
}
