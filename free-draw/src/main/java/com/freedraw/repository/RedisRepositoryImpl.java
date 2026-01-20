package com.freedraw.repository;

import com.freedraw.entities.Draft;
import com.freedraw.exception.DraftNotFoundException;
import com.freedraw.resources.RedisClient;
import com.freenote.app.server.util.JSONUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RedisRepositoryImpl implements DraftRepository {
    private static final Logger log = LogManager.getLogger(RedisRepositoryImpl.class);
    private static final String DRAFT_COLLECTION_KEY = "free_draw_draft_collections";
    
    private final RedissonClient redissonClient;

    public RedisRepositoryImpl() {
        this.redissonClient = RedisClient.getRedissonClient();
        log.info("RedisRepositoryImpl initialized with collection key: {}", DRAFT_COLLECTION_KEY);
    }

    @Override
    public Draft getDraftById(String draftId) {
        if (draftId == null || draftId.isEmpty()) {
            log.warn("Attempted to get draft with null or empty draftId");
            throw new IllegalArgumentException("Draft ID cannot be null or empty");
        }

        try {
            RMap<String, String> map = redissonClient.getMap(DRAFT_COLLECTION_KEY);
            String draftJson = map.get(draftId);
            
            if (draftJson == null) {
                log.warn("Draft not found with ID: {}", draftId);
                throw new DraftNotFoundException("Draft not found with ID: " + draftId);
            }
            
            Draft draft = JSONUtils.fromJSON(draftJson, Draft.class);
            log.debug("Retrieved draft with ID: {}", draftId);
            return draft;
        } catch (Exception e) {
            log.error("Error retrieving draft with ID {}: {}", draftId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve draft", e);
        }
    }

    @Override
    public void save(Draft draft) {
        if (draft == null) {
            log.warn("Attempted to save null draft");
            throw new IllegalArgumentException("Draft cannot be null");
        }

        if (draft.getDraftId() == null || draft.getDraftId().isEmpty()) {
            log.warn("Attempted to save draft with null or empty draftId");
            throw new IllegalArgumentException("Draft ID cannot be null or empty");
        }

        try {
            RMap<String, String> map = redissonClient.getMap(DRAFT_COLLECTION_KEY);
            String draftJson = JSONUtils.toJSONString(draft);
            
            // Use put instead of putIfAbsent to allow updates
            String previousValue = map.put(draft.getDraftId(), draftJson);
            
            if (previousValue == null) {
                log.info("Created new draft with ID: {}", draft.getDraftId());
            } else {
                log.info("Updated existing draft with ID: {}", draft.getDraftId());
            }
        } catch (Exception e) {
            log.error("Error saving draft with ID {}: {}", draft.getDraftId(), e.getMessage(), e);
            throw new RuntimeException("Failed to save draft", e);
        }
    }

    public void delete(String draftId) {
        if (draftId == null || draftId.isEmpty()) {
            log.warn("Attempted to delete draft with null or empty draftId");
            throw new IllegalArgumentException("Draft ID cannot be null or empty");
        }

        try {
            RMap<String, String> map = redissonClient.getMap(DRAFT_COLLECTION_KEY);
            String removed = map.remove(draftId);
            
            if (removed != null) {
                log.info("Deleted draft with ID: {}", draftId);
            } else {
                log.warn("Attempted to delete non-existent draft with ID: {}", draftId);
                throw new DraftNotFoundException("Draft not found with ID: " + draftId);
            }
        } catch (Exception e) {
            log.error("Error deleting draft with ID {}: {}", draftId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete draft", e);
        }
    }

    public boolean exists(String draftId) {
        if (draftId == null || draftId.isEmpty()) {
            return false;
        }

        try {
            RMap<String, String> map = redissonClient.getMap(DRAFT_COLLECTION_KEY);
            return map.containsKey(draftId);
        } catch (Exception e) {
            log.error("Error checking existence of draft with ID {}: {}", draftId, e.getMessage(), e);
            return false;
        }
    }

    public List<Draft> getAllDrafts() {
        try {
            RMap<String, String> map = redissonClient.getMap(DRAFT_COLLECTION_KEY);
            List<Draft> drafts = new ArrayList<>();
            
            for (String draftJson : map.values()) {
                Draft draft = JSONUtils.fromJSON(draftJson, Draft.class);
                if (draft != null) {
                    drafts.add(draft);
                }
            }
            
            log.debug("Retrieved {} drafts from Redis", drafts.size());
            return drafts;
        } catch (Exception e) {
            log.error("Error retrieving all drafts: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve all drafts", e);
        }
    }

    public Set<String> getAllDraftIds() {
        try {
            RMap<String, String> map = redissonClient.getMap(DRAFT_COLLECTION_KEY);
            return map.keySet();
        } catch (Exception e) {
            log.error("Error retrieving all draft IDs: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve draft IDs", e);
        }
    }

    public long count() {
        try {
            RMap<String, String> map = redissonClient.getMap(DRAFT_COLLECTION_KEY);
            return map.size();
        } catch (Exception e) {
            log.error("Error counting drafts: {}", e.getMessage(), e);
            return 0;
        }
    }

    public void clear() {
        try {
            RMap<String, String> map = redissonClient.getMap(DRAFT_COLLECTION_KEY);
            map.clear();
            log.warn("Cleared all drafts from Redis");
        } catch (Exception e) {
            log.error("Error clearing drafts: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to clear drafts", e);
        }
    }
}
