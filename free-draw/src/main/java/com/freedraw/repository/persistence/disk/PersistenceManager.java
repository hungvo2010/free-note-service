package com.freedraw.repository.persistence.disk;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static otel.GlobalOpenTelemetryManualInstrumentationUsage.sampleTelemetry;

/**
 * Factory and lifecycle manager for persistence components.
 * Handles initialization and coordination between components following IoC principles.
 */
public class PersistenceManager {
    private static final Logger log = LogManager.getLogger(PersistenceManager.class);
    private static final Tracer tracer = sampleTelemetry.getTracer();
    private static final DoubleHistogram flushLatency = sampleTelemetry.getMeter()
            .histogramBuilder("persistence.flush.duration")
            .setDescription("Latency of disk flush operations")
            .setUnit("ms")
            .build();

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
        Span span = tracer.spanBuilder("persistence.create")
                .setAttribute("db.system", "file")
                .setAttribute("db.operation", "init")
                .setAttribute("app.persistence.config", config.toString())
                .startSpan();

        try {
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
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
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
        Span span = tracer.spanBuilder("persistence.start")
                .setAttribute("db.system", "file")
                .setAttribute("db.operation", "start")
                .setAttribute("app.persistence.scheduler.active", true)
                .startSpan();
        try {
            log.info("Starting persistence manager");
            scheduler.start();
        } finally {
            span.end();
        }
    }
    
    /**
     * Stop periodic persistence
     */
    public void stop() {
        Span span = tracer.spanBuilder("persistence.stop")
                .setAttribute("db.system", "file")
                .setAttribute("db.operation", "stop")
                .setAttribute("app.persistence.scheduler.active", false)
                .startSpan();
        try {
            log.info("Stopping persistence manager");
            scheduler.stop();
        } finally {
            span.end();
        }
    }
    
    /**
     * Force immediate flush
     */
    public void flush() {
        long startTime = System.nanoTime();
        Span span = tracer.spanBuilder("persistence.flush")
                .setAttribute("db.system", "file")
                .setAttribute("db.operation", "flush")
                .setAttribute(AttributeKey.stringKey("app.persistence.mode"), "immediate")
                .startSpan();
        try {
            scheduler.flushToDisk();
            span.setStatus(StatusCode.OK);
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            long durationNs = System.nanoTime() - startTime;
            double durationMs = durationNs / 1_000_000.0;
            
            // Record duration on span as an attribute (standard practice)
            span.setAttribute("app.persistence.duration_ms", durationMs);
            
            // Record on histogram for P99/aggregation
            flushLatency.record(durationMs, Attributes.of(
                    AttributeKey.stringKey("db.system"), "file",
                    AttributeKey.stringKey("db.operation"), "flush"
            ));
            
            span.end();
        }
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
