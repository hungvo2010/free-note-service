## Why

The data access layer in `free-draw` is implicit and inconsistent: the primary store is in-memory with periodic file flush, Redis persists whole-draft JSON blobs, and no relational store with ACID guarantees exists. The store is hardcoded in `FreeNoteEndpoint` (`new InMemDraftRepositoryImpl()`), so switching stores requires code changes and the message-to-DB path is undocumented.

## What Changes

- **Add PostgreSQL data store**: `PostgresDraftRepositoryImpl` implementing the existing `DraftRepository` contract using the same entities (`Draft`, `DraftAction`, `ShapeData`).
- **Add store bootstrap**: `PostgresClient` (HikariCP pool, mirrors `RedisClient`), `postgres.properties`, and `sql/schema.sql` with DDL + seed data (executed on first init).
- **Add pluggable store selection**: `DraftRepositoryFactory` registering `memory | redis | postgres`, selected via `-Dstore.type` / `STORE_TYPE` (not wired into the endpoint yet).
- **Document the data flow**: message → DTO → service → DAO → DB in the design.

## Capabilities

### New Capabilities
- `data-access-layer`: Defines the DAO contract (`DraftRepository`), a PostgreSQL implementation, and a pluggable store factory for `free-draw`.

### Modified Capabilities
<!-- No existing specs to modify -->

## Impact

- **free-draw**: new `repository/PostgresDraftRepositoryImpl.java`, `resources/PostgresClient.java`, `repository/factory/DraftRepositoryFactory.java`, resources (`sql/schema.sql`, `postgres.properties`), `build.gradle` dependencies (`postgresql`, `HikariCP`).
- **Infra**: `postgres:16` service (db `freedraw`) added to `docker-compose.yml`.
- **No entity changes** and **no wiring changes** to `FreeNoteEndpoint`.
