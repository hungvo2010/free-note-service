# Quick Start: Apicurio Registry Integration

## 🚀 Quick Setup (5 minutes)

### 1. Start Apicurio Registry
```bash
docker-compose up -d
```

### 2. Register Your AsyncAPI Schema
```bash
chmod +x register-schemas.sh
./register-schemas.sh
```

### 3. Verify in Browser
Open: http://localhost:8080/ui/artifacts/com.freedraw/FreeNoteAPI

### 4. Build Your Application
```bash
./gradlew :free-draw:build
```

### 5. (Optional) Register Schemas via Gradle
```bash
./gradlew :free-draw:registerSchemas
```

## ✅ Verification

Check if registry is running:
```bash
curl http://localhost:8080/health
```

List all registered schemas:
```bash
curl http://localhost:8080/apis/registry/v2/groups/com.freedraw/artifacts
```

Get your AsyncAPI spec:
```bash
curl http://localhost:8080/apis/registry/v2/groups/com.freedraw/artifacts/FreeNoteAPI
```

## 📝 What Was Configured

1. **Docker Compose** (`docker-compose.yml`)
   - Runs Apicurio Registry on port 8080
   - Uses in-memory storage (for development)

2. **Registration Script** (`register-schemas.sh`)
   - Registers `asyncapi.yaml` to the registry
   - Group: `com.freedraw`
   - Artifact: `FreeNoteAPI`
   - Version: `1.0.0`

3. **Gradle Dependencies** (`free-draw/build.gradle`)
   - `apicurio-registry-client` - REST client for registry
   - `apicurio-registry-serdes-jsonschema-serde` - JSON schema serialization

4. **Java Client** (`SchemaRegistryClient.java`)
   - Singleton client for accessing registry
   - Validates messages against registered schemas

5. **AsyncAPI Spec** (`asyncapi.yaml`)
   - Contains `x-schema-registry` metadata
   - References registry URL and artifact IDs

## 🔧 Usage in Your Code

```java
import com.freedraw.registry.SchemaRegistryClient;

// Get registry client
var client = SchemaRegistryClient.getClient();

// Validate a message
SchemaRegistryClient.validateSchema(
    "com.freedraw", 
    "DraftRequestData", 
    jsonMessage
);
```

## 🛠️ Available Gradle Tasks

```bash
# Register schemas
./gradlew :free-draw:registerSchemas

# Validate AsyncAPI spec
./gradlew :free-draw:validateAsyncAPI

# Build with schema registration
./gradlew :free-draw:build registerSchemas
```

## 🌐 Registry Endpoints

- **UI**: http://localhost:8080/ui
- **API**: http://localhost:8080/apis/registry/v2
- **Health**: http://localhost:8080/health
- **Metrics**: http://localhost:8080/metrics

## 📚 Next Steps

1. Read the full setup guide: `SCHEMA_REGISTRY_SETUP.md`
2. Integrate schema validation in your WebSocket endpoint
3. Set up CI/CD to auto-register schemas on deployment
4. Configure production registry with persistent storage

## 🐛 Troubleshooting

**Registry not starting?**
```bash
docker-compose logs apicurio-registry
```

**Can't register schema?**
```bash
# Check if registry is healthy
curl http://localhost:8080/health

# Validate YAML syntax
yamllint asyncapi.yaml
```

**Connection refused in app?**
```bash
# Set registry URL
export APICURIO_REGISTRY_URL=http://localhost:8080/apis/registry/v2
```
