## MODIFIED Requirements

### Requirement: Callers must not depend on Socket or SocketChannel
No code that consumes `WebSocketSession` SHALL import or reference `java.net.Socket` or `java.nio.channels.SocketChannel` for the purpose of I/O or lifecycle management on the session's connection. All such operations SHALL go through `NetworkRequestData`. This applies equally to blocking-path handlers (`DefaultLegacySessionBasedConnectionHandler`) and NIO-path handlers (`NIOIncomingSocketHandler`).

In particular, `NIOIncomingSocketHandler.routeToHandler()` SHALL NOT call `channel.socket().getOutputStream()` to construct an `OutputWrapper`. The `OutputWrapper` SHALL be constructed from a `NetworkRequestData`-backed output stream or replaced with `networkRequestData.write()` calls.

#### Scenario: Handler reads frames without socket reference
- **WHEN** a legacy handler processes a session
- **THEN** it reads data via `session.getNetworkRequestData().read()` and checks liveness via `session.getNetworkRequestData().isClosed()`

#### Scenario: NIO handler writes without channel reference
- **WHEN** `NIOIncomingSocketHandler` needs to write a handshake response
- **THEN** it calls `networkRequestData.write(bytes)` instead of constructing a `ByteBuffer` and calling `channel.write()`

#### Scenario: NIO handler routes without channel.socket()
- **WHEN** `NIOIncomingSocketHandler` routes to a `URIEndpointHandler`
- **THEN** the `OutputWrapper` is constructed from `networkRequestData` via `new OutputWrapper(networkRequestData)` — NOT via `channel.socket().getOutputStream()`

### Requirement: OutputWrapper accepts NetworkRequestData
`OutputWrapper` SHALL provide a constructor that accepts `NetworkRequestData`, internally creating an `OutputStream` backed by `networkRequestData.write(byte[])`. This eliminates the need for `channel.socket().getOutputStream()` without changing the `URIEndpointHandler` interface.

#### Scenario: Construct OutputWrapper from NetworkRequestData
- **WHEN** `new OutputWrapper(networkRequestData)` is called
- **THEN** an `OutputWrapper` is created whose `outputStream()` writes bytes via `networkRequestData.write(byte[])`

#### Scenario: Write through NetworkRequestData-backed OutputWrapper
- **WHEN** a handler calls `outputWrapper.outputStream().write(bytes)`
- **THEN** the bytes are written to the network via `networkRequestData.write(bytes)` without accessing any raw channel or socket

### Requirement: ModernIncomingConnectionHandler interface removed
The `ModernIncomingConnectionHandler` interface SHALL be removed. `ConnectionState` SHALL call `NIOIncomingSocketHandler` directly as a concrete type. Only one implementation exists; the interface is unnecessary indirection.

#### Scenario: ConnectionState calls handler directly
- **WHEN** `ConnectionState.handle()` needs to invoke the handler
- **THEN** it calls methods on `NIOIncomingSocketHandler` directly, without casting from `ModernIncomingConnectionHandler`