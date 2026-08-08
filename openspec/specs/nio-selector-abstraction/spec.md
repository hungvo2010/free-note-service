## Purpose

Define how the NIO Selector loop, ConnectionPipeline, ConnectionState, and IncomingConnectionHandler collaborate to decouple raw I/O from business handler logic. ConnectionPipeline owns connection lifecycle and handler orchestration, while NIOServerSession is a thin transport layer.

## Requirements

### Requirement: NIO Selector loop owns all raw I/O

A dedicated IO thread SHALL run the `Selector.select()` loop and own all raw I/O operations (`channel.read()`, `channel.write()`, `ByteBuffer` allocation and lifecycle, `SelectionKey` management). No business handler SHALL call `channel.read()`, `channel.write()`, or `key.attach()` directly.

The selector loop SHALL call `NIONetworkRequestData.readFromChannel()` and `prepareForRead()` during `handleReadableEvent()`, BEFORE dispatching to the state machine. If `readFromChannel()` returns -1 (EOF), the selector loop SHALL close the connection and clean up state without dispatching to the handler.

#### Scenario: New connection accepted by selector loop

- **WHEN** a `SelectionKey.OP_ACCEPT` event fires
- **THEN** the selector loop accepts the connection, registers the channel for `OP_READ`, allocates a `ByteBuffer`, and creates initial state

#### Scenario: Data read from channel before dispatch

- **WHEN** a `SelectionKey.OP_READ` event fires
- **THEN** the selector loop calls `networkRequestData.readFromChannel()` to fill the buffer, then `networkRequestData.prepareForRead()` to flip it, THEN dispatches to the state machine with an already-filled `NIONetworkRequestData`

#### Scenario: EOF on channel read

- **WHEN** `readFromChannel()` returns -1
- **THEN** the selector loop closes the `NIONetworkRequestData`, removes channel state/buffer maps, and cancels the `SelectionKey` — without calling the handler

#### Scenario: Write ready on a channel

- **WHEN** a `SelectionKey.OP_WRITE` event fires
- **THEN** the selector loop drains pending writes from the channel's write queue via `NIONetworkRequestData.write()`

### Requirement: Dispatcher translates I/O events to business invocations

A Dispatcher SHALL receive typed events from the selector loop and invoke the appropriate business handler. The Dispatcher SHALL NOT perform I/O operations. Business handlers SHALL NOT receive `SocketChannel`, `SelectionKey`, or raw `ByteBuffer` references.

#### Scenario: Dispatch handshake to handler

- **WHEN** initial bytes are read from a newly accepted channel
- **THEN** the Dispatcher calls `handshake(upgradeRequest)` on the handler, passing only a `HttpUpgradeRequest` — not a `SocketChannel`

#### Scenario: Dispatch message to handler

- **WHEN** subsequent bytes are read from an established connection
- **THEN** the Dispatcher calls `handle(networkRequestData, outputWrapper)` on the resolved `URIEndpointHandler`, using `NetworkRequestData` and `OutputWrapper` (no raw channel)

#### Scenario: Handler throws during dispatch

- **WHEN** a handler throws a `ConnectionException`
- **THEN** the Dispatcher closes the `NetworkRequestData` and the selector loop unregisters the channel

### Requirement: Selector loop uses NetworkRequestData for all channel access

The selector loop SHALL interact with channels exclusively through `NIONetworkRequestData`. It SHALL NOT call `channel.read()` or `channel.write()` directly — those calls SHALL be encapsulated inside `NIONetworkRequestData.readFromChannel()`, `prepareForRead()`, and `NIONetworkRequestData.write()`.

#### Scenario: Read bytes via NetworkRequestData

- **WHEN** the selector loop needs to read from a channel
- **THEN** it calls `networkRequestData.readFromChannel()` which internally calls `channel.read(byteBuffer)`, followed by `networkRequestData.prepareForRead()` to flip the buffer

#### Scenario: Write bytes via NetworkRequestData

- **WHEN** the selector loop needs to write to a channel
- **THEN** it calls `networkRequestData.write(byte[])` which internally performs the `ByteBuffer.wrap` and `channel.write` loop

#### Scenario: Close connection via NetworkRequestData

- **WHEN** a connection needs to be closed (error, client disconnect, handler completion)
- **THEN** the selector loop calls `networkRequestData.close()` which closes the `SocketChannel` and cancels the `SelectionKey`

### Requirement: NIOIncomingSocketHandler delegates I/O, does not perform it

`NIOIncomingSocketHandler` SHALL delegate all I/O to `NetworkRequestData` and SHALL NOT contain `channel.read()`, `channel.write()`, `ByteBuffer.wrap()`, or `ByteBuffer.clear()` calls. It SHALL NOT cast `NetworkRequestData` to `NIONetworkRequestData`. It SHALL NOT call `readFromChannel()` or `prepareForRead()` — those calls belong to the selector loop.

#### Scenario: Handler reads without casting

- **WHEN** `NIOIncomingSocketHandler.handle()` needs to read data
- **THEN** it calls `networkRequestData.read()` and `networkRequestData.readFrameBytes()` from the `NetworkRequestData` interface, without casting to `NIONetworkRequestData`

#### Scenario: Write handshake response delegated

- **WHEN** `NIOIncomingSocketHandler` needs to write a handshake response
- **THEN** it calls `networkRequestData.write(httpResponseBytes)` instead of `ByteBuffer.wrap()` + `channel.write()` loop

#### Scenario: Close on error delegated

- **WHEN** a connection error occurs
- **THEN** `NIOIncomingSocketHandler` calls `networkRequestData.close()` instead of `channel.close()`

#### Scenario: No NIO-specific method calls in handler

- **WHEN** reviewing `NIOIncomingSocketHandler` source
- **THEN** there are zero occurrences of `readFromChannel()`, `prepareForRead()`, `(NIONetworkRequestData)`, or `java.nio.ByteBuffer` imports

### Requirement: ConnectionState transitions without handler dependency

The `ConnectionState` interface SHALL have exactly one method: `ConnectionState transition(ConnectionContext context)`. It SHALL NOT accept an `IncomingConnectionHandler` parameter and SHALL NOT call `handler.handle()`. The handler invocation is orchestrated by `ConnectionPipeline` before calling `transition()`.

`HandShakeState.transition()` SHALL inspect `context.getReadableContext().isHandshakeComplete()` to decide whether to return `MessageState` or `this`.

`MessageState.transition()` SHALL inspect `context.getNetworkRequestData().isClosed()` to decide whether to return `null` or `this`.

#### Scenario: HandShakeState transitions after successful handshake

- **WHEN** `HandShakeState.transition(context)` is called after `ConnectionPipeline` has invoked the handler and the handler has set `HttpUpgradeRequest` on `ReadableContext`
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

### Requirement: ConnectionPipeline owns state-to-data mapping

`ConnectionPipeline` SHALL maintain an internal `Map<NetworkRequestData, ConnectionState>` collection. State SHALL NOT be stored on `SelectionKey` via `key.attach()`. State SHALL NOT be stored in `NIOServerSession` or `NIOServerBootstrap`. The pipeline SHALL manage state transitions by storing the `ConnectionState.transition()` return value.

#### Scenario: State transition after handshake

- **WHEN** `HandShakeState.transition()` returns a `MessageState`
- **THEN** `ConnectionPipeline` replaces the connection's entry in the internal state map with the new `MessageState`

#### Scenario: State removed on close

- **WHEN** `transition()` returns null (connection closed)
- **THEN** `ConnectionPipeline` removes the connection's entry from the state map

#### Scenario: State initialized lazily

- **WHEN** `process()` is first called for a `NetworkRequestData`
- **THEN** a new `HandShakeState` is created via `computeIfAbsent` — the transport does not pre-create state

### Requirement: MessageState drops ByteBuffer field

`MessageState` SHALL NOT hold a `ByteBuffer` field. The buffer SHALL be managed by `NIOServerSession` and passed to the handler via `NetworkRequestData` at call time.

#### Scenario: MessageState constructed without ByteBuffer

- **WHEN** `new MessageState(httpUpgradeRequest)` is called
- **THEN** a valid `MessageState` is created with only the `HttpUpgradeRequest` — no `ByteBuffer` reference

### Requirement: NIOServerSession is a thin transport layer

`NIOServerSession` SHALL only perform transport-level operations: read bytes from channels, manage `NIONetworkRequestData` instances, delegate to `ConnectionPipeline`, and clean up channels. It SHALL NOT know about handshake state, message state, `ConnectionContext`, `ReadableContext`, `TracingContext`, or `Span`.

#### Scenario: Session delegates processing

- **WHEN** bytes are successfully read from a channel
- **THEN** `NIOServerSession` calls `pipeline.process(networkData)` and cleans up the channel only if the pipeline returns false

#### Scenario: Session reads bytes via NetworkRequestData

- **WHEN** the selector loop fires a read event
- **THEN** `NIOServerSession` calls `networkData.readFromChannel()` and `networkData.prepareForRead()` before dispatching to the pipeline

#### Scenario: Session notifies pipeline on disconnect

- **WHEN** `readFromChannel()` returns -1 (EOF) or throws an `IOException`
- **THEN** `NIOServerSession` calls `pipeline.disconnect(networkData)` before cleaning up the channel

### Requirement: ConnectionPipeline orchestrates handler and state

`ConnectionPipeline` SHALL call `handler.handle(context)` then `state.transition(context)` in sequence. It SHALL catch exceptions from either call and treat them as connection-closing events. It SHALL manage the OpenTelemetry span lifecycle, ending the span in a `finally` block.

#### Scenario: Pipeline calls handler before state transition

- **WHEN** `pipeline.process(networkData)` is called
- **THEN** `handler.handle(context)` is called first (business logic), then `state.transition(context)` is called (lifecycle decision)

#### Scenario: Pipeline ends span on exception

- **WHEN** `handler.handle(context)` throws an exception
- **THEN** the span is ended in the `finally` block, the state is removed, and `false` is returned

#### Scenario: Pipeline closes connection on handler error

- **WHEN** any exception is thrown during `handler.handle()` or `state.transition()`
- **THEN** the pipeline removes the connection's state and returns `false` to signal the transport to clean up

### Requirement: Span is always ended

The OpenTelemetry span SHALL be ended in a `finally` block within `ConnectionPipeline.process()`. The span SHALL NOT leak when handler or transition throws.

#### Scenario: Span ended on success path

- **WHEN** `handler.handle()` and `state.transition()` complete normally
- **THEN** `span.end()` is called in the `finally` block after the state map is updated

#### Scenario: Span ended on error path

- **WHEN** `handler.handle()` throws an exception
- **THEN** `span.end()` is still called in the `finally` block before the method returns false