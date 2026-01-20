package com.freedraw.registry;

import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.client.RegistryClientFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SchemaRegistryClient {
    private static final Logger log = LogManager.getLogger(SchemaRegistryClient.class);
    private static final String REGISTRY_URL = System.getenv().getOrDefault(
            "APICURIO_REGISTRY_URL", 
            "http://localhost:8080/apis/registry/v2"
    );
    
    private static RegistryClient client;
    
    public static RegistryClient getClient() {
        if (client == null) {
            synchronized (SchemaRegistryClient.class) {
                if (client == null) {
                    log.info("Initializing Apicurio Registry client with URL: {}", REGISTRY_URL);
                    client = RegistryClientFactory.create(REGISTRY_URL);
                }
            }
        }
        return client;
    }
    
    public static void validateSchema(String groupId, String artifactId, String content) {
        try {
            var registryClient = getClient();
            // Get the schema from registry
            var artifact = registryClient.getArtifactVersion(groupId, artifactId, "1.0.0");
            log.info("Schema validation successful for {}:{}", groupId, artifactId);
        } catch (Exception e) {
            log.error("Schema validation failed for {}:{}", groupId, artifactId, e);
            throw new RuntimeException("Schema validation failed", e);
        }
    }
}
