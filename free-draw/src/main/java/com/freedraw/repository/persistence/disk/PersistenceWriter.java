package com.freedraw.repository.persistence.disk;

import com.freedraw.entities.Draft;

/**
 * Interface for writing drafts to persistent storage.
 * Allows for different persistence implementations and easier testing.
 */
public interface PersistenceWriter {
    /**
     * Persist a draft to disk
     */
    void persist(Draft draft);
}
