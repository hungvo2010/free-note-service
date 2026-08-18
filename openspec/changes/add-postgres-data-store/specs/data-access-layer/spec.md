## ADDED Requirements

### Requirement: PostgreSQL data store
The system SHALL provide a PostgreSQL-backed draft store implementing the
`DraftRepository` contract (`getDraftById`, `save`) that persists the existing
`Draft`/`DraftAction` entities.

#### Scenario: Store a new draft
- **WHEN** `save` is called with a new draft
- **THEN** a `draft` row is inserted and all actions are written to
  `draft_action` rows in order, and `action_count` equals the number of
  actions

#### Scenario: Append actions to an existing draft
- **WHEN** `save` is called with a draft that already has stored actions
- **THEN** only actions beyond the stored `action_count` are inserted with
  sequential `seq` values, and `action_count` is bumped accordingly

#### Scenario: Load an existing draft
- **WHEN** `getDraftById` is called with a stored draft id
- **THEN** the draft row and all actions ordered by `seq` are returned as a
  `Draft` with the same entity types

#### Scenario: Load a missing draft
- **WHEN** `getDraftById` is called with an unknown id
- **THEN** `null` is returned

### Requirement: Store bootstrap and schema
The system SHALL initialize the PostgreSQL schema and seed reference data on
first use, idempotently.

#### Scenario: First initialization
- **WHEN** `PostgresClient` is initialized for the first time
- **THEN** tables `app_user`, `tool`, `draft`, `draft_action` are created and
  the `tool` dictionary (rectangle, circle, line, text, pen, eraser) plus a
  default `dev-user` are seeded

#### Scenario: Re-initialization
- **WHEN** schema initialization runs again against existing tables
- **THEN** no errors occur and seed rows are not duplicated

### Requirement: Pluggable store selection
The system SHALL select the draft store implementation by name via
`DraftRepositoryFactory`, with an unknown name falling back to the in-memory
store.

#### Scenario: Select a store by name
- **WHEN** `DraftRepositoryFactory.create("postgres")` is called
- **THEN** a `PostgresDraftRepositoryImpl` is returned

#### Scenario: Unknown store name
- **WHEN** `DraftRepositoryFactory.create("unknown")` is called
- **THEN** an `InMemDraftRepositoryImpl` is returned and a warning is logged
