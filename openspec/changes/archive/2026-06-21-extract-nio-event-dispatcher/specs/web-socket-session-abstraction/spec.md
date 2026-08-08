## ADDED Requirements

### Requirement: NIOIncomingSocketHandler uses handle(ConnectionContext) as entry point

`NIOIncomingSocketHandler.handle(ConnectionContext)` SHALL be the real entry point for the NIO path, called by `ConnectionState` through the shared `IncomingConnectionHandler` interface. It SHALL NOT be dead code. The handler SHALL internally determine whether to perform handshake or message processing based on whether the handshake has already completed for this connection.

#### Scenario: Handler called for handshake via shared interface

- **WHEN** `HandShakeState` calls `handler.handle(context)` on a newly accepted connection
- **THEN** the handler parses the HTTP upgrade request from the network data, performs the WebSocket handshake, and stores the parsed `HttpUpgradeRequest` on the `ReadableContext` for subsequent events

#### Scenario: Handler called for message processing via shared interface

- **WHEN** `ProcessingState` calls `handler.handle(context)` on an established connection
- **THEN** the handler reads the stored `HttpUpgradeRequest` from the context and routes to the appropriate `URIEndpointHandler`

#### Scenario: Shared interface is the only dispatch path

- **WHEN** the NIO state machine dispatches an event
- **THEN** it calls `IncomingConnectionHandler.handle(ConnectionContext)` — it SHALL NOT call `handShake()` or `handleInComingMessage()` on the concrete type

### Requirement: ReadableContext carries HttpUpgradeRequest

`ReadableContext` SHALL carry an `HttpUpgradeRequest` field, set after the handshake completes. This allows the state machine to determine state transitions after `IncomingConnectionHandler.handle()` returns void, and allows the handler to know whether a connection is in handshake or processing mode.

#### Scenario: HttpUpgradeRequest stored after handshake

- **WHEN** `NIOIncomingSocketHandler.handle()` completes a WebSocket handshake
- **THEN** the parsed `HttpUpgradeRequest` is available via `context.getReadableContext().getHttpUpgradeRequest()`

#### Scenario: HttpUpgradeRequest absent before handshake

- **WHEN** `HandShakeState` calls `context.getReadableContext().isHandshakeComplete()` after `handle()` returns but the handshake could not be completed (e.g., incomplete data)
- **THEN** `isHandshakeComplete()` returns false, signaling the handshake is not yet complete
