package com.freedraw.repository.persistence.disk;

import lombok.Builder;
import lombok.Getter;

/**
 * Configuration for disk persistence behavior
 */
@Getter
@Builder
public class DiskPersistenceConfig {
    
    /**
     * How often to flush dirty data to disk (in seconds)
     */
    @Builder.Default
    private final long flushIntervalSeconds = 30;
    
    /**
     * Whether to flush data on JVM shutdown
     */
    @Builder.Default
    private final boolean flushOnShutdown = true;
    
    /**
     * Maximum number of retry attempts for failed persistence operations
     */
    @Builder.Default
    private final int maxRetries = 3;
    
    /**
     * Whether to enable compression for persisted data
     */
    @Builder.Default
    private final boolean enableCompression = false;
    
    /**
     * Whether to enable periodic compaction of orphaned data
     */
    @Builder.Default
    private final boolean enableCompaction = false;
    
    /**
     * Compaction interval in seconds (only if enableCompaction is true)
     */
    @Builder.Default
    private final long compactionIntervalSeconds = 3600; // 1 hour
    
    public static DiskPersistenceConfig getDefault() {
        return DiskPersistenceConfig.builder().build();
    }
    
    public static DiskPersistenceConfig fastFlush() {
        return DiskPersistenceConfig.builder()
            .flushIntervalSeconds(5)
            .build();
    }
    
    public static DiskPersistenceConfig slowFlush() {
        return DiskPersistenceConfig.builder()
            .flushIntervalSeconds(300) // 5 minutes
            .build();
    }
}
