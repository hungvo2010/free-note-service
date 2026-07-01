## ADDED Requirements

### Requirement: ConnectionPipeline is transport-agnostic

`ConnectionPipeline` SHALL operate on the `NetworkRequestData` interface only. It SHALL NOT import, reference, or cast to `NIONetworkRequestData`, `SocketChannel`, `SelectionKey`, `ByteBuffer`, or any other `java.nio` type. This ensures the pipeline can be reused with any transport that provides a `NetworkRequestData` implementation.

#### Scenario: Pipeline compiles without NIO imports

- **WHEN** reviewing `ConnectionPipeline` source
- **THEN** there are zero imports from `java.nio` and zero references to `NIONetworkRequestData`

#### Scenario: Pipeline accepts any NetworkRequestData

- **WHEN** a new transport provides a `NetworkRequestData` implementation
- **THEN** `ConnectionPipeline.process()` accepts it without code changes

### Requirement: ConnectionPipeline manages connection lifecycle

`ConnectionPipeline` SHALL track each connection's lifecycle state (`HandShakeState` → `MessageState` → close) in an internal map keyed by `NetworkRequestData`. When `process()` returns `false`, the transport SHALL close the channel and the pipeline SHALL have already removed the state.

#### Scenario: First data on new connection creates HandShakeState

- **WHEN** `process()` is called for the first time on a `NetworkRequestData` instance
- **THEN** a new `HandShakeState` is created and stored in the internal map

#### Scenario: State persists across read events

- **WHEN** subsequent `process()` calls are made for the same `NetworkRequestData`
- **THEN** the existing state is retrieved from the map (not recreated)

#### Scenario: Disconnect removes state

- **WHEN** `disconnect(networkData)` is called
- **THEN** the connection's state is removed from the internal map

### Requirement: Pipeline returns keepAlive signal to transport

`process()` SHALL return `boolean`: `true` means "keep connection alive, re-register for reads", `false` means "connection is done, clean up". The transport SHALL NOT interpret the state machine directly — it only acts on this boolean signal.

#### Scenario: Keep alive after successful message processing

- **WHEN** `handler.handle()` completes and `MessageState.transition()` returns `this`
- **THEN** `process()` returns `true`, signaling the transport to keep the connection alive

#### Scenario: Close after handler error

- **WHEN** `handler.handle()` throws an exception
- **THEN** `process()` returns `false`, signaling the transport to close the connection
