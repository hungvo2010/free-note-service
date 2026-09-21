# The in-memory store is the only wired Draft store

Status: accepted

`FreeNoteEndpoint` constructs `InMemDraftRepositoryImpl` and nothing overrides it, so this is the
store the server runs on in every environment, including the containerized stack.
`DraftRepositoryFactory` registers `memory`, `redis` and `postgres`, and
`PostgresDraftRepositoryImpl` and `RedisRepositoryImpl` are implemented and schema-complete — but
`DraftRepositoryFactory.create()` has no callers, so `-Dstore.type` and `STORE_TYPE` are read by a
factory nothing invokes. The injection point exists and is likewise unused in production:
`FreeNoteEndpoint.setDraftRepository(...)` is the seam through which a store can be supplied, and
no production caller supplies one.

Redis is still used at runtime — for pub/sub routing and presence claims — just not to hold Drafts.

## Why not switch now

The three adapters are not yet substitutable, so selecting a different store would change
behaviour, not just location:

- **Missing Draft.** `RedisRepositoryImpl.getDraftById` throws `DraftNotFoundException`;
  `InMemDraftRepositoryImpl` and `PostgresDraftRepositoryImpl` return `null`.
- **`save`.** Redis serialises the whole Draft and overwrites the key; Postgres appends only the
  actions beyond the stored count; the in-memory adapter stores a live reference that the disk
  engine later rewrites at the vector tail.

Both are tractable — `docs/STORAGE_SCALABILITY_DESIGN.md` specifies the append-only write contract
and the `null`-for-missing read contract — but neither is done.

## Consequences

Production writes to `/tmp/freenote_data` inside the container: `deploy/config/application.properties`
leaves `freenote.target.directory` empty, and `docker-compose.prod.yml` mounts only
`./deploy/config`. Drafts therefore live in the container's writable layer and do not survive a
container recreation — including the `docker compose up -d --build` that `run.sh` runs. Losing
Drafts on redeploy is expected while this ADR stands, not a defect to report.

Note also that the in-memory adapter is not a plain map: it is backed by a hand-rolled file engine,
so every Draft shares one process-wide `ReentrantReadWriteLock` and one 30-second flush thread, and
the store choice also decides a filesystem location and a background thread.

## Considered options

Switching production to Postgres — the only adapter whose write path matches the domain, since an
Action is an append-only event and `appendNewActions` writes only the delta, and the target of the
storage design doc. Deferred, not rejected: it depends on the contract work above.
