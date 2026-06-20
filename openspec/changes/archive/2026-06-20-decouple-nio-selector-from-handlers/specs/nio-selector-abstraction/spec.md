## ADDED Requirements

### Requirement: NIO Selector loop owns all raw I/O
A dedicated IO thread SHALL run the `Selector.select()` loop and own all raw I/O operations (`channel.read()`, `channel.write()`, `ByteBuffer` allocation and lifecycle, `SelectionKey` management). No business handler SHALL call `channel.read()`, `channel.write()`, or `key.attach()` directly.

#### Scenario: New connection accepted by selector loop
- **WHEN** a `SelectionKey.OP_ACCEPT` event fires
- **THEN** the selector loop accepts the connection, registers the channel for `OP_READ`, and creates a `NIONetworkRequestData` wrapping the channel

#### Scenario: Data ready on a channel
- **WHEN** a `SelectionKey.OP_READ` event fires
- **THEN** the selector loop reads bytes into a `ByteBuffer`, wraps them in a `NIONetworkRequestData`, and hands off to the Dispatcher without exposing the `SocketChannel` or `SelectionKey`

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
The selector loop SHALL interact with channels exclusively through `NIONetworkRequestData`. It SHALL NOT call `channel.read()` or `channel.write()` directly — those calls SHALL be encapsulated inside `NIONetworkRequestData.read()` and `NIONetworkRequestData.write()`.

#### Scenario: Read bytes via NetworkRequestData
- **WHEN** the selector loop needs to read from a channel
- **THEN** it calls `networkRequestData.read(byteBuffer)` which internally calls `channel.read(byteBuffer)`

#### Scenario: Write bytes via NetworkRequestData
- **WHEN** the selector loop needs to write to a channel
- **THEN** it calls `networkRequestData.write(byte[])` which internally performs the `ByteBuffer.wrap` and `channel.write` loop

#### Scenario: Close connection via NetworkRequestData
- **WHEN** a connection needs to be closed (error, client disconnect, handler completion)
- **THEN** the selector loop calls `networkRequestData.close()` which closes the `SocketChannel` and cancels the `SelectionKey`

### Requirement: NIOIncomingSocketHandler delegates I/O, does not perform it
`NIOIncomingSocketHandler` SHALL delegate all I/O to `NetworkRequestData` and SHALL NOT contain `channel.read()`, `channel.write()`, `ByteBuffer.wrap()`, or `ByteBuffer.clear()` calls.

#### Scenario: Read empty check delegated
- **WHEN** checking if a read from a channel returned EOF
- **THEN** `NIOIncomingSocketHandler` calls `networkRequestData.read(byteBuffer)` and checks the return value, without calling `channel.read()` directly

#### Scenario: Write handshake response delegated
- **WHEN** sending a handshake response
- **THEN** `NIOIncomingSocketHandler` calls `networkRequestData.write(httpResponseBytes)` instead of `ByteBuffer.wrap()` + `channel.write()` loop

#### Scenario: Close on error delegated
- **WHEN** a connection error occurs
- **THEN** `NIOIncomingSocketHandler` calls `networkRequestData.close()` instead of `channel.close()`

### Requirement: ConnectionState interface simplified to pure dispatch
The `ConnectionState` interface SHALL have exactly one method: `ConnectionState handle(NIOIncomingSocketHandler handler, NetworkRequestData networkData) throws IOException`. It SHALL NOT expose `ByteBuffer getByteBuffer()`, `decodeString()`, or `handleCloseBracket()` — those are removed.

#### Scenario: Handle returns next state
- **WHEN** a `HandShakeState.handle()` successfully completes the handshake
- **THEN** it returns a new `ProcessingState(request)` — the selector loop stores the returned state for the next I/O event

#### Scenario: Handle stays in same state
- **WHEN** a `HandShakeState.handle()` needs more data to complete parsing
- **THEN** it returns `this` — the selector loop keeps the current state for the next read event

#### Scenario: ByteBuffer removed from ConnectionState
- **WHEN** a `ConnectionState` implementation needs a buffer for I/O
- **THEN** the buffer is provided by `NetworkRequestData` at call time — `ConnectionState` has no `getByteBuffer()` method

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
