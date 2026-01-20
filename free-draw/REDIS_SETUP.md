# Redis Integration Setup Guide

## Overview
This guide explains the Redis integration for the Free Draw application, which uses Redisson client to store and manage Draft entities.

## Components

### 1. RedisClient (`com.freedraw.resources.RedisClient`)
Singleton class that manages the Redisson client connection.

**Features:**
- Thread-safe singleton pattern with double-checked locking
- Configuration loaded from `redis.properties`
- Connection pooling support
- Graceful shutdown method

### 2. RedisRepositoryImpl (`com.freedraw.repository.RedisRepositoryImpl`)
Implementation of `DraftRepository` interface using Redis as the storage backend.

**Methods:**
- `save(Draft draft)` - Save or update a draft
- `getDraftById(String draftId)` - Retrieve a draft by ID
- `delete(String draftId)` - Delete a draft
- `exists(String draftId)` - Check if a draft exists
- `getAllDrafts()` - Get all drafts
- `getAllDraftIds()` - Get all draft IDs
- `count()` - Get total number of drafts
- `clear()` - Clear all drafts (use with caution)

### 3. Configuration (`redis.properties`)
Located at: `free-draw/src/main/resources/redis.properties`

**Properties:**
```properties
redis.host=157.66.219.174
redis.port=6379
redis.password=
redis.database=0
redis.timeout=3000
redis.connection.pool.size=64
redis.connection.minimum.idle.size=10
```

## Setup Instructions

### 1. Verify Dependencies
The `free-draw/build.gradle` already includes the Redisson dependency:
```gradle
implementation("org.redisson:redisson:4.1.0")
```

### 2. Configure Redis Connection
Edit `free-draw/src/main/resources/redis.properties` to match your Redis server:
```properties
redis.host=your-redis-host
redis.port=6379
redis.password=your-password-if-any
```

### 3. Build the Project
```bash
cd free-draw
../gradlew build
```

### 4. Run the Example
```bash
../gradlew run -PmainClass=com.freedraw.example.RedisRepositoryExample
```

Or from the root project:
```bash
./gradlew :free-draw:run -PmainClass=com.freedraw.example.RedisRepositoryExample
```

### 5. Run Tests
```bash
../gradlew test --tests RedisRepositoryImplTest
```

## Usage Examples

### Basic Usage
```java
// Initialize repository
RedisRepositoryImpl repository = new RedisRepositoryImpl();

// Create and save a draft
Draft draft = new Draft();
repository.save(draft);

// Retrieve a draft
Draft retrieved = repository.getDraftById(draft.getDraftId());

// Update a draft
Draft updated = new Draft(draft.getDraftId(), "New Name");
repository.save(updated);

// Delete a draft
repository.delete(draft.getDraftId());

// Cleanup
RedisClient.shutdown();
```

### Using in DraftService
Update `DraftService` to use Redis instead of in-memory storage:

```java
public class DraftService {
    private final DraftRepository draftRepository = new RedisRepositoryImpl();
    
    // Your existing methods will now use Redis
}
```

## Data Structure in Redis

**Key:** `free_draw_draft_collections`  
**Type:** Hash (RMap)  
**Structure:**
```
free_draw_draft_collections = {
    "draft-id-1": "{\"draftId\":\"draft-id-1\",\"draftName\":\"Draft 1\"}",
    "draft-id-2": "{\"draftId\":\"draft-id-2\",\"draftName\":\"Draft 2\"}",
    ...
}
```

## Error Handling

The repository includes comprehensive error handling:
- `IllegalArgumentException` - For null or empty parameters
- `DraftNotFoundException` - When a draft is not found
- `RuntimeException` - For Redis connection or operation failures

All errors are logged with appropriate log levels.

## Testing

### Unit Tests
Run the test suite:
```bash
../gradlew test --tests RedisRepositoryImplTest
```

### Manual Testing
Use the example class to test manually:
```bash
../gradlew run -PmainClass=com.freedraw.example.RedisRepositoryExample
```

### Redis CLI Testing
Connect to Redis and verify data:
```bash
redis-cli -h 157.66.219.174 -p 6379
> HGETALL free_draw_draft_collections
> HLEN free_draw_draft_collections
```

## Troubleshooting

### Connection Issues
1. Verify Redis server is running: `redis-cli ping`
2. Check firewall rules for port 6379
3. Verify host and port in `redis.properties`
4. Check logs for connection errors

### Data Not Persisting
1. Verify Redis persistence is enabled (RDB or AOF)
2. Check Redis logs for errors
3. Verify sufficient disk space

### Performance Issues
1. Adjust connection pool size in `redis.properties`
2. Monitor Redis memory usage
3. Consider using Redis clustering for high load

## Production Considerations

1. **Security:**
   - Enable Redis authentication (set password)
   - Use TLS/SSL for connections
   - Restrict network access to Redis

2. **High Availability:**
   - Use Redis Sentinel or Redis Cluster
   - Configure Redisson for cluster mode

3. **Monitoring:**
   - Monitor Redis memory usage
   - Track connection pool metrics
   - Set up alerts for connection failures

4. **Backup:**
   - Enable RDB snapshots
   - Configure AOF persistence
   - Regular backup schedule

## Additional Resources

- [Redisson Documentation](https://github.com/redisson/redisson/wiki)
- [Redis Documentation](https://redis.io/documentation)
- [Redis Best Practices](https://redis.io/topics/best-practices)
