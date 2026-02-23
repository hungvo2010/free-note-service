# Apicurio Registry Setup Guide

This guide explains how to set up and use Apicurio Registry with your FreeNote AsyncAPI specification.

## Prerequisites

- Docker and Docker Compose installed
- curl and jq installed (for registration script)

## Step 1: Start Apicurio Registry

Start the registry using Docker Compose:

```bash
docker-compose up -d
```

Wait for the registry to be healthy:

```bash
docker-compose ps
```

The registry UI will be available at: http://localhost:8080/ui

## Step 2: Register Your AsyncAPI Schema

Make the registration script executable and run it:

```bash
chmod +x register-schemas.sh
./register-schemas.sh
```

This will:
- Register your `asyncapi.yaml` file to the registry
- Create it under group `com.freedraw` with artifact ID `FreeNoteAPI`
- Set the version to `1.0.0`

## Step 3: Verify Registration

### Via UI
Open http://localhost:8080/ui/artifacts/com.freedraw/FreeNoteAPI

### Via API
```bash
curl http://localhost:8080/apis/registry/v2/groups/com.freedraw/artifacts/FreeNoteAPI
```

## Step 4: Register Individual Message Schemas

You can also register the individual message schemas (DraftRequestData, DraftResponseData):

```bash
# Extract and register DraftRequestData schema
curl -X POST "http://localhost:8080/apis/registry/v2/groups/com.freedraw/artifacts" \
  -H "Content-Type: application/json" \
  -H "X-Registry-ArtifactId: DraftRequestData" \
  -H "X-Registry-ArtifactType: JSON" \
  -H "X-Registry-Version: 1.0.0" \
  -d '{
    "type": "object",
    "required": ["requestType"],
    "properties": {
      "draftId": {"type": "string"},
      "draftName": {"type": "string"},
      "requestType": {"type": "integer", "enum": [0,1,2,3,4,5]},
      "content": {"type": "object", "additionalProperties": true},
      "shapes": {
        "type": "array",
        "items": {
          "type": "object",
          "required": ["shapeId"],
          "properties": {
            "shapeId": {"type": "string"},
            "type": {"type": "string"},
            "content": {"type": "object", "additionalProperties": true}
          }
        }
      }
    }
  }'

# Register DraftResponseData schema
curl -X POST "http://localhost:8080/apis/registry/v2/groups/com.freedraw/artifacts" \
  -H "Content-Type: application/json" \
  -H "X-Registry-ArtifactId: DraftResponseData" \
  -H "X-Registry-ArtifactType: JSON" \
  -H "X-Registry-Version: 1.0.0" \
  -d '{
    "type": "object",
    "properties": {
      "draftId": {"type": "string"},
      "draftName": {"type": "string"},
      "actionType": {"type": "integer", "enum": [0,1,2,3,4,5,-1]},
      "shapes": {
        "type": "array",
        "items": {
          "type": "object",
          "required": ["shapeId"],
          "properties": {
            "shapeId": {"type": "string"},
            "type": {"type": "string"},
            "content": {"type": "object", "additionalProperties": true}
          }
        }
      }
    }
  }'
```

## Step 5: Use in Your Application

### Environment Variable
Set the registry URL (optional, defaults to localhost:8080):

```bash
export APICURIO_REGISTRY_URL=http://localhost:8080/apis/registry/v2
```

### In Code
The `SchemaRegistryClient` class provides access to the registry:

```java
import com.freedraw.registry.SchemaRegistryClient;

// Get the registry client
var client = SchemaRegistryClient.getClient();

// Validate against schema
SchemaRegistryClient.validateSchema("com.freedraw", "DraftRequestData", jsonContent);
```

## Common Operations

### List all artifacts
```bash
curl http://localhost:8080/apis/registry/v2/groups/com.freedraw/artifacts
```

### Get specific artifact
```bash
curl http://localhost:8080/apis/registry/v2/groups/com.freedraw/artifacts/FreeNoteAPI
```

### Get artifact metadata
```bash
curl http://localhost:8080/apis/registry/v2/groups/com.freedraw/artifacts/FreeNoteAPI/meta
```

### Update schema (new version)
```bash
curl -X POST "http://localhost:8080/apis/registry/v2/groups/com.freedraw/artifacts/FreeNoteAPI/versions" \
  -H "Content-Type: application/x-yaml" \
  -H "X-Registry-Version: 1.0.1" \
  --data-binary @asyncapi.yaml
```

### Delete artifact
```bash
curl -X DELETE "http://localhost:8080/apis/registry/v2/groups/com.freedraw/artifacts/FreeNoteAPI"
```

## Integration with Build Process

Add to your `build.gradle`:

```gradle
task registerSchemas(type: Exec) {
    commandLine 'bash', 'register-schemas.sh'
}

// Run after build
build.finalizedBy registerSchemas
```

## Troubleshooting

### Registry not starting
Check logs:
```bash
docker-compose logs apicurio-registry
```

### Schema registration fails
1. Ensure registry is running: `curl http://localhost:8080/health`
2. Check YAML syntax: `yamllint asyncapi.yaml`
3. Verify the file path in the script

### Connection refused in application
1. Check `APICURIO_REGISTRY_URL` environment variable
2. Ensure registry is accessible from your application
3. Check network connectivity

## Production Considerations

For production, consider:

1. **Persistent Storage**: Use PostgreSQL or Kafka storage instead of in-memory
   ```yaml
   services:
     postgres:
       image: postgres:14
       environment:
         POSTGRES_DB: registry
         POSTGRES_USER: registry
         POSTGRES_PASSWORD: registry
     
     apicurio-registry:
       image: apicurio/apicurio-registry-sql:latest
       environment:
         REGISTRY_DATASOURCE_URL: jdbc:postgresql://postgres:5432/registry
         REGISTRY_DATASOURCE_USERNAME: registry
         REGISTRY_DATASOURCE_PASSWORD: registry
   ```

2. **Authentication**: Enable OAuth or basic auth
3. **High Availability**: Run multiple registry instances
4. **Monitoring**: Set up health checks and metrics
5. **Backup**: Regular backups of registry data

## References

- [Apicurio Registry Documentation](https://www.apicur.io/registry/docs/)
- [AsyncAPI Specification](https://www.asyncapi.com/docs/reference/specification/v3.0.0)
- [REST API Reference](https://www.apicur.io/registry/docs/apicurio-registry/2.5.x/getting-started/assembly-using-the-registry-rest-api.html)
