## 1. Dependencies and Infrastructure

- [ ] 1.1 Add `org.postgresql:postgresql:42.7.3` and `com.zaxxer:HikariCP:5.1.0` to `free-draw/build.gradle`
- [ ] 1.2 Add `postgres:16` service (db `freedraw`) to `docker-compose.yml`

## 2. Resources and Client

- [ ] 2.1 Create `free-draw/src/main/resources/postgres.properties`
- [ ] 2.2 Create `free-draw/src/main/resources/sql/schema.sql` with the exact DDL from design.md Decision 2: `app_user`, `tool`, `draft` (with `creator_id` FK + `idx_draft_creator`), `draft_action`, plus idempotent seed inserts (`tool` ×6, `app_user` ×1)
- [ ] 2.3 Create `com.freedraw.resources.PostgresClient` (HikariCP singleton, loads properties, runs schema on first init)

## 3. PostgreSQL Repository

- [ ] 3.1 Create `com.freedraw.repository.PostgresDraftRepositoryImpl` implementing `DraftRepository`: `getDraftById` (draft row + actions by seq, `null` if absent)
- [ ] 3.2 Implement `save` (one transaction: upsert draft, read `action_count`, append actions beyond it with `seq = count..size-1`, bump count; rollback on failure; no creator handling in this change)

## 4. Pluggable Store Factory

- [ ] 4.1 Create `com.freedraw.repository.factory.DraftRepositoryFactory` with registry `memory | redis | postgres`, selection via `-Dstore.type` / `STORE_TYPE` / default `memory`

## 5. Documentation and Verification

- [ ] 5.1 Add "How to add a new data store" section to `docs/STORAGE_SCALABILITY_DESIGN.md`
- [ ] 5.2 Verify: `gradlew.bat :free-draw:compileJava`
- [ ] 5.3 Optional smoke test: `docker compose up -d postgres`, run server with `-Dstore.type=postgres`, draw shapes, confirm rows in `draft_action`
