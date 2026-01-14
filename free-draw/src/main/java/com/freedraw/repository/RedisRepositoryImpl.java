package com.freedraw.repository;

import com.freedraw.entities.Draft;
import com.freedraw.resources.RedisClient;
import com.freenote.app.server.util.JSONUtils;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

public class RedisRepositoryImpl implements DraftRepository {
    private RedissonClient redissonClient = RedisClient.getRedissonClient();

    @Override
    public Draft getDraftById(String draftId) {
        RMap<String, String> map = redissonClient.getMap("free_draw_draft_collections");
        return JSONUtils.fromJSON(map.get(draftId), Draft.class);
    }

    @Override
    public void save(Draft draft) {
        RMap<String, String> map = redissonClient.getMap("free_draw_draft_collections");
        map.putIfAbsent(draft.getDraftId(), JSONUtils.toJSONString(draft));
    }
}
