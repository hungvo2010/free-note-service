package com.freedraw.repository;

import com.freedraw.entities.Draft;
import com.freedraw.entities.DraftAction;
import com.freedraw.repository.persistence.disk.DiskPersistenceConfig;
import com.freedraw.repository.persistence.disk.PersistenceContext;
import com.freedraw.repository.persistence.disk.PersistenceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;

/**
 * In-memory draft repository with periodic disk persistence.
 * All operations happen in memory, with automatic background flushing to disk.
 */
public class InMemDraftRepositoryImpl implements DraftRepository {
    private static final Logger log = LogManager.getLogger(InMemDraftRepositoryImpl.class);
    
    private final PersistenceManager persistenceManager;
    private final PersistenceContext persistenceContext;

    /**
     * Create repository with default persistence configuration
     */
    public InMemDraftRepositoryImpl() {
        this(DiskPersistenceConfig.getDefault());
    }
    
    /**
     * Create repository with custom persistence configuration
     */
    public InMemDraftRepositoryImpl(DiskPersistenceConfig config) {
        log.info("Initializing InMemDraftRepository with persistence");
        
        // Create and start persistence manager
        this.persistenceManager = PersistenceManager.create(config);
        this.persistenceContext = persistenceManager.getContext();
        
        // Start periodic persistence
        this.persistenceManager.start();
        
        log.info("Repository initialized with {} drafts", 
            persistenceContext.getAllDrafts().size());
    }

    @Override
    public Draft getDraftById(String draftId) {
        Optional<Draft> draft = persistenceContext.getDraft(draftId);
        return draft.orElse(null);
    }

    @Override
    public void save(Draft draft) {
        // Save to in-memory store (will be persisted periodically)
        persistenceContext.saveDraft(draft);
        log.debug("Draft {} saved to memory", draft.getDraftId());
    }
    
    /**
     * Get all drafts from memory
     */
    public List<Draft> getAllDrafts() {
        return persistenceContext.getAllDrafts();
    }
    
    /**
     * Add an action to a draft
     */
    public void addAction(String draftId, DraftAction action) {
        persistenceContext.addAction(draftId, action);
        log.debug("Action added to draft {}", draftId);
    }
    
    /**
     * Insert an action at a specific position
     */
    public void insertAction(String draftId, int position, DraftAction action) {
        persistenceContext.insertAction(draftId, position, action);
        log.debug("Action inserted at position {} in draft {}", position, draftId);
    }
    
    /**
     * Force immediate flush to disk
     */
    public void flush() {
        persistenceManager.flush();
        log.info("Forced flush to disk");
    }
    
    /**
     * Shutdown the repository and persist remaining data
     */
    public void shutdown() {
        log.info("Shutting down repository");
        persistenceManager.stop();
    }
}
