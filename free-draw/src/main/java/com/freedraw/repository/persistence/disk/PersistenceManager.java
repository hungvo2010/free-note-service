package com.freedraw.repository.persistence.disk;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Factory and lifecycle manager for persistence components.
 * Handles initialization and coordination between components following IoC principles.
 */
public class PersistenceManager {
    private static final Logger log = LogManager.getLogger(PersistenceManager.class);
    
    private final PersistenceContext persistenceContext;
    private final DiskPersistenceScheduler scheduler;
    
    private PersistenceManager(PersistenceContext persistenceContext, 
                              DiskPersistenceScheduler scheduler) {
        this.persistenceContext = persistenceContext;
        this.scheduler = scheduler;
    }
    
    /**
     * Create and initialize all persistence components
     */
    public static PersistenceManager create(DiskPersistenceConfig config) {
        log.info("Creating persistence manager");
        
        // 1. Create and initialize persistence context
        PersistenceContext context = new PersistenceContext();
        context.initData();
        
        // 2. Create scheduler with injected dependencies
        DiskPersistenceScheduler scheduler = new DiskPersistenceScheduler(
            context,  // PersistenceWriter interface
            context.getInMemoryStore(),
            config
        );
        
        log.info("Persistence manager created successfully");
        return new PersistenceManager(context, scheduler);
    }
    
    /**
     * Create with default configuration
     */
    public static PersistenceManager createDefault() {
        return create(DiskPersistenceConfig.getDefault());
    }
    
    /**
     * Start periodic persistence
     */
    public void start() {
        log.info("Starting persistence manager");
        scheduler.start();
    }
    
    /**
     * Stop periodic persistence
     */
    public void stop() {
        log.info("Stopping persistence manager");
        scheduler.stop();
    }
    
    /**
     * Force immediate flush
     */
    public void flush() {
        scheduler.flushToDisk();
    }
    
    /**
     * Get the persistence context for data operations
     */
    public PersistenceContext getContext() {
        return persistenceContext;
    }
    
    /**
     * Check if scheduler is running
     */
    public boolean isRunning() {
        return scheduler.isRunning();
    }
}
