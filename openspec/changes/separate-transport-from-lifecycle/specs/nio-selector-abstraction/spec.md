## MODIFIED Requirements

### Requirement: ConnectionState transitions without handler dependency

The `ConnectionState` interface SHALL have exactly one method: `ConnectionState transition(ConnectionContext context)`. It SHALL NOT accept an `IncomingConnectionHandler` parameter and SHALL NOT call `handler.handle()`. The handler invocation is orchestrated by `NIOConnectionProcessor` before calling `transition()`.

`HandShakeState.transition()` SHALL inspect `context.getReadableContext().isHandshakeComplete()` to decide whether to return `MessageState` or `this`.

`MessageState.transition()` SHALL inspect `context.getNetworkRequestData().isClosed()` to decide whether to return `null` or `this`.

#### Scenario: HandShakeState transitions after successful handshake

- **WHEN** `HandShakeState.transition(context)` is called after `NIOConnectionProcessor` has invoked the handler and the handler has set `HttpUpgradeRequest` on `ReadableContext`
- **THEN** `isHandshakeComplete()` returns true, and the method returns a new `MessageState` carrying the `HttpUpgradeRequest`

#### Scenario: HandShakeState stays when handshake incomplete

- **WHEN** `HandShakeState.transition(context)` is called but the handler has not yet set `HttpUpgradeRequest` (insufficient data for full HTTP upgrade request)
- **THEN** `isHandshakeComplete()` returns false, and the method returns `this`

#### Scenario: MessageState returns null when channel is closed

- **WHEN** `MessageState.transition(context)` is called and `context.getNetworkRequestData().isClosed()` returns true
- **THEN** the method returns `null`, signaling the connection should be cleaned up

#### Scenario: MessageState stays when channel is open

- **WHEN** `MessageState.transition(context)` is called and `context.getNetworkRequestData().isClosed()` returns false
- **THEN** the method returns `this`

### Requirement: NIOConnectionProcessor owns state-to-data mapping

`NIOConnectionProcessor` SHALL maintain an internal `Map<NetworkRequestData, ConnectionState>` collection. State SHALL NOT be stored on `SelectionKey` via `key.attach()`. State SHALL NOT be stored in `NIOServerSession` or `NIOServerBootstrap`. The processor SHALL manage state transitions by storing the `ConnectionState.transition()` return value.

#### Scenario: State transition after handshake

- **WHEN** `HandShakeState.transition()` returns a `MessageState`
- **THEN** `NIOConnectionProcessor` replaces the connection's entry in the internal state map with the new `MessageState`

#### Scenario: State removed on close

- **WHEN** `transition()` returns null (connection closed)
- **THEN** `NIOConnectionProcessor` removes the connection's entry from the state map

#### Scenario: State initialized lazily

- **WHEN** `onData()` is first called for a `NetworkRequestData`
- **THEN** a new `HandShakeState` is created via `computeIfAbsent` — the transport does not pre-create state

### Requirement: NIOServerSession is a thin transport layer

`NIOServerSession` SHALL only perform transport-level operations: read bytes from channels, manage `NIONetworkRequestData` instances, delegate to `NIOConnectionProcessor`, and clean up channels. It SHALL NOT know about handshake state, message state, `ConnectionContext`, `ReadableContext`, `TracingContext`, or `Span`.

#### Scenario: Session delegates processing

- **WHEN** bytes are successfully read from a channel
- **THEN** `NIOServerSession` calls `processor.onData(networkData)` and cleans up the channel only if the processor returns false

#### Scenario: Session reads bytes via NetworkRequestData

- **WHEN** the selector loop fires a read event
- **THEN** `NIOServerSession` calls `networkData.readFromChannel()` and `networkData.prepareForRead()` before dispatching to the processor

#### Scenario: Session notifies processor on disconnect

- **WHEN** `readFromChannel()` returns -1 (EOF) or throws an `IOException`
- **THEN** `NIOServerSession` calls `processor.onDisconnect(networkData)` before cleaning up the channel

## ADDED Requirements

### Requirement: NIOConnectionProcessor orchestrates handler and state

`NIOConnectionProcessor` SHALL call `handler.handle(context)` then `state.transition(context)` in sequence. It SHALL catch exceptions from either call and treat them as connection-closing events. It SHALL manage the OpenTelemetry span lifecycle, ending the span in a `finally` block.

#### Scenario: Processor calls handler before state transition

- **WHEN** `processor.onData(networkData)` is called
- **THEN** `handler.handle(context)` is called first (business logic), then `state.transition(context)` is called (lifecycle decision)

#### Scenario: Processor ends span on exception

- **WHEN** `handler.handle(context)` throws an exception
- **THEN** the span is ended in the `finally` block, the state is removed, and `false` is returned

#### Scenario: Processor closes connection on handler error

- **WHEN** any exception is thrown during `handler.handle()` or `state.transition()`
- **THEN** the processor removes the connection's state and returns `false` to signal the transport to clean up

### Requirement: Span is always ended

The OpenTelemetry span SHALL be ended in a `finally` block within `NIOConnectionProcessor.onData()`. The span SHALL NOT leak when handler or transition throws.

#### Scenario: Span ended on success path

- **WHEN** `handler.handle()` and `state.transition()` complete normally
- **THEN** `span.end()` is called in the `finally` block after the state map is updated

#### Scenario: Span ended on error path

- **WHEN** `handler.handle()` throws an exception
- **THEN** `span.end()` is still called in the `finally` block before the method returns false
