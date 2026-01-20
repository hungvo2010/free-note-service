package com.freedraw.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Validates JSON messages against schemas fetched from Apicurio Registry
 */
public class RegistrySchemaValidator {
    private static final Logger log = LogManager.getLogger(RegistrySchemaValidator.class);
    private static final String REGISTRY_URL = "http://157.66.219.174:9081/apis/registry/v2";
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final YAMLMapper yamlMapper = new YAMLMapper();
    private static final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    
    private JsonSchema requestSchema;
    private JsonSchema responseSchema;
    
    public RegistrySchemaValidator() {
        loadSchemasFromRegistry();
    }
    
    /**
     * Fetch AsyncAPI spec from registry and extract schemas
     */
    private void loadSchemasFromRegistry() {
        try {
            log.info("Fetching AsyncAPI spec from registry...");
            String asyncApiYaml = fetchFromRegistry("/groups/com.freedraw/artifacts/FreeNoteAPI");
            
            if (asyncApiYaml == null) {
                log.error("Failed to fetch AsyncAPI spec from registry");
                return;
            }
            
            // Parse YAML to JSON
            JsonNode asyncApiDoc = yamlMapper.readTree(asyncApiYaml);
            
            // Get all schemas
            JsonNode allSchemas = asyncApiDoc.path("components").path("schemas");
            
            // Extract and resolve request schema
            JsonNode requestSchemaNode = asyncApiDoc
                .path("components")
                .path("schemas")
                .path("draftRequestPayload");
            
            JsonNode responseSchemaNode = asyncApiDoc
                .path("components")
                .path("schemas")
                .path("draftResponsePayload");
            
            if (!requestSchemaNode.isMissingNode()) {
                // Resolve $ref references
                JsonNode resolvedRequestSchema = resolveReferences(requestSchemaNode, allSchemas);
                this.requestSchema = factory.getSchema(resolvedRequestSchema);
                log.info("✓ Request schema loaded from registry");
            } else {
                log.warn("Request schema not found in AsyncAPI spec");
            }
            
            if (!responseSchemaNode.isMissingNode()) {
                // Resolve $ref references
                JsonNode resolvedResponseSchema = resolveReferences(responseSchemaNode, allSchemas);
                this.responseSchema = factory.getSchema(resolvedResponseSchema);
                log.info("✓ Response schema loaded from registry");
            } else {
                log.warn("Response schema not found in AsyncAPI spec");
            }
            
        } catch (Exception e) {
            log.error("Failed to load schemas from registry", e);
        }
    }
    
    /**
     * Resolve $ref references in schema
     */
    private JsonNode resolveReferences(JsonNode schema, JsonNode allSchemas) {
        try {
            ObjectNode resolved = schema.deepCopy();
            resolveReferencesRecursive(resolved, allSchemas);
            return resolved;
        } catch (Exception e) {
            log.error("Failed to resolve references", e);
            return schema;
        }
    }
    
    /**
     * Recursively resolve all $ref in a schema node
     */
    private void resolveReferencesRecursive(JsonNode node, JsonNode allSchemas) {
        if (node.isObject()) {
            ObjectNode objNode = (ObjectNode) node;
            
            // Check if this node has a $ref
            if (objNode.has("$ref")) {
                String ref = objNode.get("$ref").asText();
                // Extract schema name from #/components/schemas/schemaName
                if (ref.startsWith("#/components/schemas/")) {
                    String schemaName = ref.substring("#/components/schemas/".length());
                    JsonNode referencedSchema = allSchemas.get(schemaName);
                    
                    if (referencedSchema != null) {
                        // Remove $ref and merge referenced schema
                        objNode.remove("$ref");
                        Iterator<Map.Entry<String, JsonNode>> fields = referencedSchema.fields();
                        while (fields.hasNext()) {
                            Map.Entry<String, JsonNode> field = fields.next();
                            objNode.set(field.getKey(), field.getValue().deepCopy());
                        }
                    }
                }
            }
            
            // Recursively process all fields
            Iterator<Map.Entry<String, JsonNode>> fields = objNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                resolveReferencesRecursive(field.getValue(), allSchemas);
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                resolveReferencesRecursive(item, allSchemas);
            }
        }
    }
    
    /**
     * Fetch content from registry using curl
     */
    private String fetchFromRegistry(String path) {
        try {
            String url = REGISTRY_URL + path;
            ProcessBuilder processBuilder = new ProcessBuilder(
                "curl", "-s", url
            );
            
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("curl command failed with exit code: {}", exitCode);
                return null;
            }
            
            return output.toString();
            
        } catch (Exception e) {
            log.error("Failed to fetch from registry: {}", path, e);
            return null;
        }
    }
    
    /**
     * Validate request message
     */
    public ValidationResult validateRequest(String jsonMessage) {
        return validate(jsonMessage, requestSchema, "DraftRequestData");
    }
    
    /**
     * Validate response message
     */
    public ValidationResult validateResponse(String jsonMessage) {
        return validate(jsonMessage, responseSchema, "DraftResponseData");
    }
    
    /**
     * Validate JSON against schema
     */
    private ValidationResult validate(String jsonMessage, JsonSchema schema, String schemaName) {
        if (schema == null) {
            String error = "Schema not loaded: " + schemaName + ". Check registry connection.";
            log.error(error);
            return new ValidationResult(false, error);
        }
        
        try {
            JsonNode jsonNode = jsonMapper.readTree(jsonMessage);
            Set<ValidationMessage> errors = schema.validate(jsonNode);
            
            if (errors.isEmpty()) {
                log.debug("✓ Validation successful for {}", schemaName);
                return new ValidationResult(true, null);
            } else {
                StringBuilder errorMsg = new StringBuilder("Schema validation failed for " + schemaName + ":\n");
                for (ValidationMessage error : errors) {
                    errorMsg.append("  - ").append(error.getMessage()).append("\n");
                }
                log.error(errorMsg.toString());
                return new ValidationResult(false, errorMsg.toString());
            }
        } catch (Exception e) {
            String error = "Validation error for " + schemaName + ": " + e.getMessage();
            log.error(error, e);
            return new ValidationResult(false, error);
        }
    }
    
    /**
     * Reload schemas from registry (useful for updates)
     */
    public void reloadSchemas() {
        log.info("Reloading schemas from registry...");
        loadSchemasFromRegistry();
    }
    
    /**
     * Validation result
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        @Override
        public String toString() {
            return valid ? "Valid" : "Invalid: " + errorMessage;
        }
    }
}
