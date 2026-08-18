# Scalable Draft Storage Design

Status: proposal
Scope: `free-draw` persistence layer
Created: 2026-08-18

## Why

The current persistence design in `free-draw` does not scale with draft size or
concurrency:

1. **Whole-draft rewrite per update** — `DraftService` performs a read-modify-write
   (`service/DraftService.java:37`), `RedisRepositoryImpl` serializes the entire
   draft graph on every save (`RedisRepositoryImpl.java:61`), and the disk store
   rewrites all actions at the vector tail on every flush
   (`PersistenceContext.java:79`). A draft with N actions costs O(N) per update,
   making a draft's total write cost quadratic over its lifetime.
2. **Single global lock** — `InMemoryDraftStore` guards all drafts with one
   `ReentrantReadWriteLock` (`InMemoryDraftStore.java:22`). All rooms contend on
   one lock; throughput does not scale with cores or rooms.
3. **No versioning / CAS** — concurrent writers to the same draft silently lose
   updates; there is no mechanism to detect or reject conflicts.
4. **Unbounded append-only vector** — old action ranges are never reclaimed; the
   `actions` file grows without bound.
5. **Single-threaded flush** (`DiskPersistenceScheduler.java:39`) — durability is
   a throughput bottleneck.
6. **No atomicity or isolation** — index, offsets, and data files are updated
   non-atomically; a crash between appends leaves dangling offsets. The file store
   cannot express the transaction semantics a multi-user whiteboard needs.

## Target Model

Normalize the draft into two records. This is a natural fit for a whiteboard:
every client action is an immutable event, and the draft is a projection.

```
draft        (draft_id PK, draft_name TEXT, version BIGINT NOT NULL DEFAULT 0, updated_at)
draft_action (draft_id FK, seq BIGINT, shapes_json JSONB, created_at,
              PRIMARY KEY (draft_id, seq))
```

- `draft.version` — optimistic concurrency token, incremented on every accepted
  action.
- `draft_action.seq` — monotonically increasing per-draft sequence; ordering is
  deterministic without a global sequence.
- `shapes_json` — the serialized `DraftAction` payload (its `shapes` list). The
  `Draft`/`DraftAction`/`ShapeData` Java entities are unchanged; they remain the
  aggregate/read model. Only the storage layout changes.

## Write Path

One transaction per accepted action:

```sql
BEGIN;
UPDATE draft SET version = version + 1, updated_at = now()
 WHERE draft_id = ? AND version = ?;          -- optimistic CAS
-- abort if 0 rows updated (conflict → retry or notify client)
INSERT INTO draft_action (draft_id, seq, shapes_json, created_at)
 VALUES (?, ?, ?, now());
COMMIT;
```

- The CAS on `version` + the `(draft_id, seq)` PK constraint give conflict
  detection without `SELECT ... FOR UPDATE` and without SERIALIZABLE isolation.
- `seq` is derived from `version` after the CAS (or assigned via
  `SELECT COALESCE(MAX(seq),0)+1` inside the same transaction with the PK
  constraint as the backstop).
- This removes the server-side read-merge-write (`generateMergedAction`) from the
  hot path: client actions are appended blindly because appends commute. The
  merge becomes a pure read-time projection.

### Isolation Level Guidance

- READ COMMITTED is sufficient for the append path above: correctness comes from
  CAS + unique constraints, not from snapshot guarantees.
- REPEATABLE READ / SERIALIZABLE is only needed if a transaction reads state and
  *decides* what to write based on it (e.g., server-side merge on CONNECT). In
  that case, prefer SERIALIZABLE and handle serialization failures with a retry,
  since the decision depends on a snapshot.
- These levels apply to the new store's DB (Postgres/H2). The file store has no
  isolation and must be retired as the primary store.

## Read Path

- **Broadcast** — tail read, O(1):
  ```sql
  SELECT shapes_json FROM draft_action
   WHERE draft_id = ? ORDER BY seq DESC LIMIT 1;
  ```
- **CONNECT snapshot** — replay, O(actions) once:
  ```sql
  SELECT seq, shapes_json FROM draft_action WHERE draft_id = ? ORDER BY seq;
  ```
  `Draft.generateMergedAction()` becomes the last step of this replay instead of
  a persisted rewrite.

## Concurrency Model

- **Per-draft lock striping** in the in-memory layer: e.g. 256 sharded
  `ReentrantLock`s (`ConcurrentHashMap<String, ReentrantLock>`), replacing the
  single global `ReadWriteLock`. Uncontended drafts never block each other.
- **Optimistic versioning** at the storage layer: CAS failure → retry with fresh
  state or reject with a conflict response; never last-writer-wins.
- The store should expose an atomic primitive instead of `save(Draft)`:

  ```java
  interface ActionLogStore {
      long appendAction(String draftId, DraftAction action); // returns new seq
      Optional<DraftAction> lastAction(String draftId);
      List<DraftAction> actionsFrom(String draftId, long afterSeq);
  }
  ```

## Compaction

- Keep the last N actions per draft (e.g. 1000).
- Periodically (background batch) collapse older actions into one snapshot row
  (`allShapes = true` style payload) at `seq = max`, then delete the collapsed
  rows. Done in one transaction → readers always see a consistent log.
- Bounds disk growth and CONNECT replay cost.

## Migration Path

1. Add `version` column to the new store's `draft` table (default 0); no change
   to Java entities.
2. Implement the new store (`ActionLogStore` + a thin `DraftRepository` adapter)
   against Postgres (reuse `docker-compose.yml`) or H2.
3. Backfill: read drafts from the existing disk/Redis stores, insert each
   `DraftAction` as a `draft_action` row in order, set `version = actions count`.
4. Feature flag / dual-write to switch traffic, then retire the file store as the
   primary path (keep it as a fallback or for tests).

## Non-Goals

- No multi-node distributed coordination (single-process assumption; state lives
  in the DB).
- No sharding across Postgres partitions or Redis cluster slots.
- No full event-sourcing framework; the action log is a lightweight event log.
- No change to the `Draft`/`DraftAction`/`ShapeData` entity API in this phase.

## How to Add a New Data Store

The store layer is pluggable via `DraftRepositoryFactory`
(`com.freedraw.repository.factory`). Adding a new store takes four steps:

1. **Implement the contract** — create `XxxRepositoryImpl implements
   DraftRepository` (`getDraftById` + `save`) using the same entities
   (`Draft`, `DraftAction`, `ShapeData`). Follow the append-only action-log
   invariant of `PostgresDraftRepositoryImpl`: `save` appends only actions
   beyond the stored count; it never rewrites earlier actions.
2. **Register it** — add one entry to `DraftRepositoryFactory.REGISTRY`
   (`store name → supplier`), e.g. `"mongo", MongoDraftRepositoryImpl::new`.
3. **Select at runtime** — start the server with `-Dstore.type=<name>` (or
   the `STORE_TYPE` env var). Unknown names fall back to `memory`.
4. **Bootstrap resources** — mirror `PostgresClient`/`PostgresDraftRepositoryImpl`:
   a client singleton with a properties file, and idempotent schema
   initialization (`CREATE TABLE IF NOT EXISTS` + `ON CONFLICT DO NOTHING`
   seeds) executed on first use.

Patterns every store must keep: same entities, append-only writes, `null`
(silent) missing-draft reads to match `InMemDraftRepositoryImpl` semantics,
and no store-side concurrency control until the design's versioning/CA
mechanism is implemented.
