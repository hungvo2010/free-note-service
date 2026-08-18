## Context

`free-draw` handles collaborative drawing. Data flow today:
WebSocket frame → `FreeNoteEndpoint.onData` → DTO (`DraftRequestData`) →
`DraftService` → `Draft` entity → `DraftRepository` → store
(`InMemDraftRepositoryImpl`/disk or `RedisRepositoryImpl`). The store is
hardcoded at the endpoint and no relational DB exists. This change adds a
PostgreSQL store with a pluggable store factory, and documents the
message-to-DB path.

## Goals / Non-Goals

**Goals:**
- PostgreSQL store implementing the existing `DraftRepository` contract
  (`getDraftById`, `save`) with the same entities (`Draft`, `DraftAction`,
  `ShapeData`).
- Pluggable store selection via `DraftRepositoryFactory`.
- Simple SQL assuming no concurrency (no `FOR UPDATE`, no CAS).
- Documented, consistent path: message → service → DAO/DAL → DB.

**Non-Goals:**
- No changes to `Draft`/`DraftAction`/`ShapeData` entities.
- No wiring of the factory into `FreeNoteEndpoint` (done in a later change).
- No optimistic/pessimistic concurrency control; no retry logic.
- No Redis schema migration.

## Decisions

### 1. Layered data flow (message → DB)

```
WebSocket frame (JSON)
  → FreeNoteEndpoint.onData                       [transport]
  → DraftRequestData (DTO)                        [message object]
  → DraftService.handleDraftRequest               [service layer]
  → Draft (aggregate)                             [domain object]
  → DraftRepository                               [DAO interface]
  → PostgresDraftRepositoryImpl                   [DAL implementation]
  → HikariCP pool (PostgresClient)
  → tables: app_user, tool, draft, draft_action   [DB]
```

### 2. Storage model (normalized, append-only action log)

Tables (DDL in `sql/schema.sql`, executed on first init, idempotent):

- `app_user (user_id PK, user_name, created_at)` — seeded with `dev-user`.
- `tool (tool_id PK, tool_name UNIQUE, created_at)` — reference dictionary,
  seeded with `rectangle/circle/line/text/pen/eraser` (matches
  `ShapeData.type` values). Standalone table: shapes embed tool names as
  strings inside `shapes_json`, so no FK is natural.
- `draft (draft_id PK, draft_name DEFAULT 'Untitled', creator_id FK →
  app_user ON DELETE SET NULL, action_count, created_at, updated_at)` —
  `creator_id` is schema-ready only; the `Draft` entity has no creator field
  and `save(Draft)` cannot see `senderId` (request-DTO only), so population
  is deferred. Index `idx_draft_creator` on `creator_id`.
- `draft_action (draft_id FK CASCADE, seq, shapes_json JSONB, created_at,
  PK (draft_id, seq))` — append-only action log.

`save(Draft)`: one transaction — upsert draft, read `action_count`, insert
only actions beyond the stored count (seq = count..size-1), bump
`action_count`. Simple SQL, no locking; concurrency control explicitly out of
scope. `getDraftById`: draft row + actions `ORDER BY seq`; returns `null`
when absent (matches `InMemDraftRepositoryImpl` semantics).

### 3. Store bootstrap

`PostgresClient` singleton mirrors `RedisClient`: loads `postgres.properties`
(defaults `localhost:5432/freedraw`, user/pass `postgres`), HikariCP pool,
executes `sql/schema.sql` (CREATE TABLE IF NOT EXISTS + seeds) on first init.

### 4. Pluggable store selection

`DraftRepositoryFactory.REGISTRY`: `memory → InMemDraftRepositoryImpl`,
`redis → RedisRepositoryImpl`, `postgres → PostgresDraftRepositoryImpl`.
Selection: `-Dstore.type` system property → `STORE_TYPE` env → default
`memory`. Adding a new store = implement `DraftRepository` + one registry
entry.

### 5. Dependencies and infra

- `org.postgresql:postgresql:42.7.3`, `com.zaxxer:HikariCP:5.1.0` in
  `free-draw/build.gradle`.
- `docker-compose.yml`: `postgres:16` service (db `freedraw`, credentials
  matching `postgres.properties` defaults).

## Risks / Trade-offs

- **[Risk]** `save` appends only actions beyond `action_count`; a caller
  sending a *modified* earlier action is silently ignored.
  → [Mitigation] Document the append-only invariant; the existing service
  flow always appends, so it holds.
- **[Risk]** `shapes_json` stores the full `DraftAction` (incl. `actionData`
  map) via `JSONUtils`; same pattern as the disk store, so behavior is
  consistent.
- **[Trade-off]** No concurrency control → last-writer races possible;
  accepted per scope (assume no concurrency), documented for future work.

## Migration Plan

- New store is additive: `DraftRepositoryFactory.create()` returns it only
  when selected via `-Dstore.type=postgres`. In-memory stays the default, so
  rollback = remove the flag.
- Schema bootstraps itself (`CREATE TABLE IF NOT EXISTS` + idempotent seeds);
  no manual migration step.
- Docker: `docker compose up -d postgres` starts the DB.

## Open Questions

- When to wire `DraftRepositoryFactory` into `FreeNoteEndpoint` (follow-up
  change).
- How to populate `draft.creator_id` (requires a `creatorId` on the `Draft`
  entity or an extended repository signature; deferred).
