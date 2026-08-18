package com.freedraw.repository.factory;

import com.freedraw.repository.DraftRepository;
import com.freedraw.repository.InMemDraftRepositoryImpl;
import com.freedraw.repository.PostgresDraftRepositoryImpl;
import com.freedraw.repository.RedisRepositoryImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Factory for pluggable draft data stores.
 *
 * To add a new data store:
 *   1. Implement DraftRepository (getDraftById + save) in a new class.
 *   2. Register it in REGISTRY below: store name -> supplier.
 *   3. Select it at runtime with the "store.type" system property
 *      (e.g. -Dstore.type=postgres) or the STORE_TYPE environment variable.
 */
public final class DraftRepositoryFactory {
    private static final Logger log = LogManager.getLogger(DraftRepositoryFactory.class);
    private static final String DEFAULT_STORE_TYPE = "memory";

    private static final Map<String, Supplier<DraftRepository>> REGISTRY = Map.of(
            "memory", InMemDraftRepositoryImpl::new,
            "redis", RedisRepositoryImpl::new,
            "postgres", PostgresDraftRepositoryImpl::new
    );

    private DraftRepositoryFactory() {
    }

    public static DraftRepository create() {
        String type = System.getProperty("store.type");
        if (type == null || type.isBlank()) {
            type = System.getenv("STORE_TYPE");
        }
        if (type == null || type.isBlank()) {
            type = DEFAULT_STORE_TYPE;
        }
        return create(type);
    }

    public static DraftRepository create(String type) {
        Supplier<DraftRepository> supplier = REGISTRY.get(type);
        if (supplier == null) {
            log.warn("Unknown store type '{}', falling back to '{}'", type, DEFAULT_STORE_TYPE);
            supplier = REGISTRY.get(DEFAULT_STORE_TYPE);
        }
        log.info("Creating draft repository of type: {}", type);
        return supplier.get();
    }
}
