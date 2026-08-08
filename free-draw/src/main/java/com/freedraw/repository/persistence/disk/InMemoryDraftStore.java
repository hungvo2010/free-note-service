package com.freedraw.repository.persistence.disk;

import com.freedraw.entities.Draft;
import com.freedraw.entities.DraftAction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * In-memory storage for drafts with dirty tracking.
 * All modifications happen in memory, solving the insertion problem.
 */
public class InMemoryDraftStore {
    private static final Logger log = LogManager.getLogger(InMemoryDraftStore.class);
    
    private final Map<String, Draft> drafts = new ConcurrentHashMap<>();
    private final Set<String> dirtyDraftIds = ConcurrentHashMap.newKeySet();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    /**
     * Load initial data from disk
     */
    public void loadFromDisk(List<Draft> draftsFromDisk) {
        lock.writeLock().lock();
        try {
            drafts.clear();
            dirtyDraftIds.clear();
            for (Draft draft : draftsFromDisk) {
                drafts.put(draft.getDraftId(), draft);
            }
            log.info("Loaded {} drafts from disk into memory", drafts.size());
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Get a draft by ID
     */
    public Optional<Draft> getDraft(String draftId) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(drafts.get(draftId));
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get all drafts
     */
    public List<Draft> getAllDrafts() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(drafts.values());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Create or update a draft
     */
    public void saveDraft(Draft draft) {
        lock.writeLock().lock();
        try {
            drafts.put(draft.getDraftId(), draft);
            markDirty(draft.getDraftId());
            log.debug("Saved draft {} to memory (marked dirty)", draft.getDraftId());
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Add an action to a draft (append)
     */
    public void addAction(String draftId, DraftAction action) {
        lock.writeLock().lock();
        try {
            Draft draft = drafts.get(draftId);
            if (draft != null) {
                draft.addAction(action);
                markDirty(draftId);
                log.debug("Added action to draft {} (marked dirty)", draftId);
            } else {
                log.warn("Cannot add action: draft {} not found", draftId);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Insert an action at a specific position
     */
    public void insertAction(String draftId, int position, DraftAction action) {
        lock.writeLock().lock();
        try {
            Draft draft = drafts.get(draftId);
            if (draft != null) {
                List<DraftAction> actions = draft.getActions();
                if (position >= 0 && position <= actions.size()) {
                    actions.add(position, action);
                    markDirty(draftId);
                    log.debug("Inserted action at position {} in draft {} (marked dirty)", position, draftId);
                } else {
                    log.warn("Invalid position {} for draft {} (size: {})", position, draftId, actions.size());
                }
            } else {
                log.warn("Cannot insert action: draft {} not found", draftId);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Remove an action at a specific position
     */
    public void removeAction(String draftId, int position) {
        lock.writeLock().lock();
        try {
            Draft draft = drafts.get(draftId);
            if (draft != null) {
                List<DraftAction> actions = draft.getActions();
                if (position >= 0 && position < actions.size()) {
                    actions.remove(position);
                    markDirty(draftId);
                    log.debug("Removed action at position {} from draft {} (marked dirty)", position, draftId);
                } else {
                    log.warn("Invalid position {} for draft {} (size: {})", position, draftId, actions.size());
                }
            } else {
                log.warn("Cannot remove action: draft {} not found", draftId);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Delete a draft
     */
    public void deleteDraft(String draftId) {
        lock.writeLock().lock();
        try {
            drafts.remove(draftId);
            dirtyDraftIds.remove(draftId);
            log.debug("Deleted draft {} from memory", draftId);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Get all drafts that have been modified since last flush
     */
    public List<Draft> getDirtyDrafts() {
        lock.readLock().lock();
        try {
            List<Draft> result = new ArrayList<>();
            for (String draftId : dirtyDraftIds) {
                Draft draft = drafts.get(draftId);
                if (draft != null) {
                    result.add(draft);
                }
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Mark a draft as clean (persisted to disk)
     */
    public void markClean(String draftId) {
        dirtyDraftIds.remove(draftId);
        log.debug("Marked draft {} as clean", draftId);
    }
    
    /**
     * Mark a draft as dirty (needs persistence)
     */
    private void markDirty(String draftId) {
        dirtyDraftIds.add(draftId);
    }
    
    /**
     * Get statistics about the in-memory store
     */
    public StoreStats getStats() {
        lock.readLock().lock();
        try {
            return new StoreStats(drafts.size(), dirtyDraftIds.size());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public record StoreStats(int totalDrafts, int dirtyDrafts) {
        @Override
        public String toString() {
            return String.format("Total: %d, Dirty: %d", totalDrafts, dirtyDrafts);
        }
    }
}
