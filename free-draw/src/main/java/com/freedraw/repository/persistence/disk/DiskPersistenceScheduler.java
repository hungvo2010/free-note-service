package com.freedraw.repository.persistence.disk;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduler that manages periodic persistence of in-memory data to disk.
 * Follows IoC principles - dependencies are injected, must be explicitly started.
 */
public class DiskPersistenceScheduler {
    private static final Logger log = LogManager.getLogger(DiskPersistenceScheduler.class);
    
    private final PersistenceWriter persistenceWriter;
    private final InMemoryDraftStore inMemoryStore;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    
    // Configuration parameters
    private final long flushIntervalSeconds;
    private final boolean flushOnShutdown;
    private final int maxRetries;
    
    /**
     * Constructor with dependency injection - does NOT auto-start
     */
    public DiskPersistenceScheduler(PersistenceWriter persistenceWriter,
                                   InMemoryDraftStore inMemoryStore,
                                   DiskPersistenceConfig config) {
        this.persistenceWriter = persistenceWriter;
        this.inMemoryStore = inMemoryStore;
        this.flushIntervalSeconds = config.getFlushIntervalSeconds();
        this.flushOnShutdown = config.isFlushOnShutdown();
        this.maxRetries = config.getMaxRetries();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "DiskPersistenceScheduler");
            thread.setDaemon(true);
            return thread;
        });
    }
    
    /**
     * Start periodic disk persistence
     */
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            log.info("Starting disk persistence scheduler with interval: {} seconds", flushIntervalSeconds);
            scheduler.scheduleAtFixedRate(
                this::flushToDisk,
                flushIntervalSeconds,
                flushIntervalSeconds,
                TimeUnit.SECONDS
            );
            
            if (flushOnShutdown) {
                registerShutdownHook();
            }
        } else {
            log.warn("Disk persistence scheduler is already running");
        }
    }
    
    /**
     * Stop the scheduler and optionally flush remaining data
     */
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            log.info("Stopping disk persistence scheduler");
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Flush all dirty drafts to disk
     */
    public void flushToDisk() {
        try {
            var dirtyDrafts = inMemoryStore.getDirtyDrafts();
            if (dirtyDrafts.isEmpty()) {
                log.debug("No dirty drafts to persist");
                return;
            }
            
            log.info("Flushing {} dirty drafts to disk", dirtyDrafts.size());
            int successCount = 0;
            int failureCount = 0;
            
            for (var draft : dirtyDrafts) {
                boolean success = persistWithRetry(draft);
                if (success) {
                    successCount++;
                    inMemoryStore.markClean(draft.getDraftId());
                } else {
                    failureCount++;
                }
            }
            
            log.info("Flush complete: {} succeeded, {} failed", successCount, failureCount);
            
        } catch (Exception e) {
            log.error("Error during disk flush: {}", e.getMessage(), e);
        }
    }
    
    private boolean persistWithRetry(com.freedraw.entities.Draft draft) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                persistenceWriter.persist(draft);
                return true;
            } catch (Exception e) {
                log.warn("Failed to persist draft {} (attempt {}/{}): {}", 
                    draft.getDraftId(), attempt, maxRetries, e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(100 * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }
        log.error("Failed to persist draft {} after {} attempts", draft.getDraftId(), maxRetries);
        return false;
    }
    
    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown hook triggered - flushing data to disk");
            flushToDisk();
        }, "DiskPersistenceShutdownHook"));
    }
    
    public boolean isRunning() {
        return isRunning.get();
    }
}
