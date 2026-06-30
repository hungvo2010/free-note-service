## ADDED Requirements

### Requirement: NIOConnectionProcessor is transport-agnostic

`NIOConnectionProcessor` SHALL operate on the `NetworkRequestData` interface only. It SHALL NOT import, reference, or cast to `NIONetworkRequestData`, `SocketChannel`, `SelectionKey`, `ByteBuffer`, or any other `java.nio` type. This ensures the processor can be reused with any transport that provides a `NetworkRequestData` implementation.

#### Scenario: Processor compiles without NIO imports

- **WHEN** reviewing `NIOConnectionProcessor` source
- **THEN** there are zero imports from `java.nio` and zero references to `NIONetworkRequestData`

#### Scenario: Processor accepts any NetworkRequestData

- **WHEN** a new transport provides a `NetworkRequestData` implementation
- **THEN** `NIOConnectionProcessor.onData()` accepts it without code changes

### Requirement: NIOConnectionProcessor manages connection lifecycle

`NIOConnectionProcessor` SHALL track each connection's lifecycle state (`HandShakeState` → `MessageState` → close) in an internal map keyed by `NetworkRequestData`. When `onData()` returns `false`, the transport SHALL close the channel and the processor SHALL have already removed the state.

#### Scenario: First data on new connection creates HandShakeState

- **WHEN** `onData()` is called for the first time on a `NetworkRequestData` instance
- **THEN** a new `HandShakeState` is created and stored in the internal map

#### Scenario: State persists across read events

- **WHEN** subsequent `onData()` calls are made for the same `NetworkRequestData`
- **THEN** the existing state is retrieved from the map (not recreated)

#### Scenario: Disconnect removes state

- **WHEN** `onDisconnect(networkData)` is called
- **THEN** the connection's state is removed from the internal map

### Requirement: Processor returns keepAlive signal to transport

`onData()` SHALL return `boolean`: `true` means "keep connection alive, re-register for reads", `false` means "connection is done, clean up". The transport SHALL NOT interpret the state machine directly — it only acts on this boolean signal.

#### Scenario: Keep alive after successful message processing

- **WHEN** `handler.handle()` completes and `MessageState.transition()` returns `this`
- **THEN** `onData()` returns `true`, signaling the transport to keep the connection alive

#### Scenario: Close after handler error

- **WHEN** `handler.handle()` throws an exception
- **THEN** `onData()` returns `false`, signaling the transport to close the connection
