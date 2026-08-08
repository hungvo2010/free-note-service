## Purpose

Define how ReadableContext is decoupled from NetworkRequestData, which is now owned by ConnectionContext. ReadableContext carries only TracingContext and HttpUpgradeRequest.

## Requirements

### Requirement: ReadableContext no longer carries NetworkRequestData

`ReadableContext` SHALL NOT carry a `NetworkRequestData` field. `ConnectionContext` already carries `NetworkRequestData` — the duplication in `ReadableContext` is unused (zero usages across the codebase) and confusing. Consumers that need `NetworkRequestData` SHALL obtain it from `ConnectionContext.getNetworkRequestData()`.

#### Scenario: ReadableContext built without NetworkRequestData

- **WHEN** `ReadableContext.builder().tracingContext(tracingContext).httpUpgradeRequest(upgradeRequest).build()` is called
- **THEN** a valid `ReadableContext` is created with only `TracingContext` and `HttpUpgradeRequest`

#### Scenario: NetworkRequestData accessed via ConnectionContext

- **WHEN** a handler needs the network data for a connection
- **THEN** it calls `context.getNetworkRequestData()` on the `ConnectionContext`, not on `ReadableContext`

### Requirement: ReadableContext must not expose SelectionKey
`ReadableContext` SHALL NOT expose `SelectionKey` to consumers. State management (`setState()`) SHALL NOT call `key.attach()` — state SHALL be managed externally by the selector loop or through a separate mechanism.

#### Scenario: State is managed externally
- **WHEN** the selector loop needs to attach processing state to a connection
- **THEN** it manages state through its own internal mapping (e.g., a `Map<NetworkRequestData, ProcessingState>`) rather than `key.attach()`

### Requirement: ReadableContext read operation via NetworkRequestData
The `emptyReadFromChannel` check SHALL be performed via `NetworkRequestData.read()` rather than direct `channel.read()`.

#### Scenario: Check for empty read
- **WHEN** checking whether a channel read returned EOF
- **THEN** `networkRequestData.read(byteBuffer)` is called, and if it returns -1, `networkRequestData.close()` is invoked
