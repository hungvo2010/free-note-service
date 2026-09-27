## Purpose

Define how `ConnectionEvent` pairs a connection's transport with the lifecycle state the connection is currently in. Replaces the former `ConnectionContext` / `ReadableContext` pair.

## Requirements

### Requirement: ConnectionEvent carries NetworkRequestData and ConnectionState
`ConnectionEvent` SHALL carry a `NetworkRequestData` instance instead of raw `Socket`, `SocketChannel`, and `ByteBuffer` fields, and SHALL carry the `ConnectionState` the connection is currently in. The raw transport fields MUST be removed.

#### Scenario: Construct ConnectionEvent with NetworkRequestData and state
- **WHEN** `ConnectionEvent.builder().networkRequestData(networkData).state(state).build()` is called
- **THEN** a valid `ConnectionEvent` is created carrying both the network abstraction and the connection state

#### Scenario: Access network operations via NetworkRequestData
- **WHEN** a consumer calls `event.getNetworkRequestData()`
- **THEN** it receives the `NetworkRequestData` instance that can perform all read, write, close, and lifecycle operations

### Requirement: ReadableContext does not exist
`ReadableContext` SHALL NOT exist. Handshake completion SHALL be derived from the `ConnectionState` on the `ConnectionEvent`, not from a nullable field on a separate per-event object.

#### Scenario: Phase comes from the state machine
- **WHEN** a handler needs to know whether the handshake has completed
- **THEN** it inspects `event.getState()`, the same state the pipeline uses to drive transitions

### Requirement: Handlers report the parsed upgrade request
`PerConnectionHandler.handle(ConnectionEvent)` SHALL return the `HttpUpgradeRequest` parsed during the handshake, or `null` when no handshake was performed. The state machine SHALL advance from `HandShakeState` to `MessageState` based on that return value.

#### Scenario: Handshake event advances the state
- **WHEN** a handler parses an upgrade request and returns it
- **THEN** `HandShakeState.transition` returns a new `MessageState` holding that request

#### Scenario: Message event does not advance
- **WHEN** the connection is already in `MessageState` and the handler returns `null`
- **THEN** `MessageState.transition` returns itself, or returns `null` to close the connection when the transport reports closed
