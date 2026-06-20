## Purpose

Define how the NIO Selector loop, ConnectionState, and IncomingConnectionHandler collaborate to decouple raw I/O from business handler logic.

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

### Requirement: ConnectionState calls IncomingConnectionHandler.handle()

The `ConnectionState` interface SHALL have exactly one method: `ConnectionState handle(IncomingConnectionHandler handler, ConnectionContext context) throws IOException`. It SHALL call `handler.handle(context)` — the shared `IncomingConnectionHandler` interface method — not NIO-specific methods like `handShake()` or `handleInComingMessage()`.

`HandShakeState` SHALL read the parsed `HttpUpgradeRequest` from `context.getReadableContext().getHttpUpgradeRequest()` after `handle()` returns to determine whether to transition to `ProcessingState`.

#### Scenario: HandShakeState dispatches through interface

- **WHEN** `HandShakeState.handle()` is called
- **THEN** it calls `handler.handle(context)` and reads the resulting `HttpUpgradeRequest` from the context to decide the next state

#### Scenario: ProcessingState dispatches through interface

- **WHEN** `ProcessingState.handle()` is called
- **THEN** it calls `handler.handle(context)` and checks `context.getNetworkRequestData().isClosed()` to decide whether to stay in processing or return null

#### Scenario: ByteBuffer removed from ConnectionState

- **WHEN** a `ConnectionState` implementation needs a buffer for I/O
- **THEN** the buffer is owned by the selector loop and filled before dispatch — `ConnectionState` has no `getByteBuffer()` method

### Requirement: NIOServerBootstrap owns state-to-channel mapping

`NIOServerBootstrap` SHALL maintain internal `Map<SocketChannel, ConnectionState>` and `Map<SocketChannel, ByteBuffer>` collections. State SHALL NOT be stored on `SelectionKey` via `key.attach()`. The selector loop SHALL manage state transitions by storing the `ConnectionState` return value.

#### Scenario: State transition after handshake

- **WHEN** `HandShakeState.handle()` returns a `ProcessingState`
- **THEN** `NIOServerBootstrap` replaces the channel's entry in the internal state map with the new `ProcessingState`

#### Scenario: State removed on close

- **WHEN** a connection is closed (error or client disconnect)
- **THEN** `NIOServerBootstrap` removes the channel's entries from both the state map and the buffer map, and cancels the `SelectionKey`

### Requirement: ProcessingState drops ByteBuffer field

`ProcessingState` SHALL NOT hold a `ByteBuffer` field. The buffer SHALL be managed by `NIOServerBootstrap` and passed to the handler via `NetworkRequestData` at call time.

#### Scenario: ProcessingState constructed without ByteBuffer

- **WHEN** `new ProcessingState(httpUpgradeRequest)` is called
- **THEN** a valid `ProcessingState` is created with only the `HttpUpgradeRequest` — no `ByteBuffer` reference