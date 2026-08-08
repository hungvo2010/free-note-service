## MODIFIED Requirements

### Requirement: ReadableContext no longer carries NetworkRequestData

`ReadableContext` SHALL NOT carry a `NetworkRequestData` field. `ConnectionContext` already carries `NetworkRequestData` — the duplication in `ReadableContext` is unused (zero usages across the codebase) and confusing. Consumers that need `NetworkRequestData` SHALL obtain it from `ConnectionContext.getNetworkRequestData()`.

#### Scenario: ReadableContext built without NetworkRequestData

- **WHEN** `ReadableContext.builder().tracingContext(tracingContext).httpUpgradeRequest(upgradeRequest).build()` is called
- **THEN** a valid `ReadableContext` is created with only `TracingContext` and `HttpUpgradeRequest`

#### Scenario: NetworkRequestData accessed via ConnectionContext

- **WHEN** a handler needs the network data for a connection
- **THEN** it calls `context.getNetworkRequestData()` on the `ConnectionContext`, not on `ReadableContext`

## REMOVED Requirements

### Requirement: ReadableContext carries NetworkRequestData instead of raw NIO types

**Removed.** The `NetworkRequestData` field on `ReadableContext` was never read. `ConnectionContext` is the canonical place to obtain `NetworkRequestData`. Removing the unused field eliminates confusion about which context object carries the transport data.
