## Purpose

Define how `WebSocketSession` delegates all network I/O to `NetworkRequestData`, how `NIOIncomingSocketHandler` serves as the shared entry point for the NIO state machine, and how `ReadableContext` carries handshake state across events.

## Requirements

### Requirement: Session delegates I/O to NetworkRequestData

`WebSocketSession` SHALL delegate all network I/O operations to its `NetworkRequestData` field. It MUST NOT expose raw `Socket` or `SocketChannel` objects to callers.

#### Scenario: Write bytes to connection

- **WHEN** `writeResponse(WebSocketFrame)` is called on a `WebSocketSession`
- **THEN** the frame bytes are written via `networkRequestData.write(byte[])` without accessing a raw socket

#### Scenario: Send handshake response

- **WHEN** `sendHandshakeResponse(HttpUpgradeResponse)` is called on a `WebSocketSession`
- **THEN** the response bytes are written via `networkRequestData.write(byte[])` without accessing a raw socket

#### Scenario: Get remote address

- **WHEN** `getRemoteAddress()` is called on a `WebSocketSession`
- **THEN** it delegates to `networkRequestData.getRemoteAddress()` and returns the result

### Requirement: Session construction without socket or channel

The `WebSocketSession` builder SHALL NOT accept a `socket` or `socketChannel` parameter. The only transport-related parameter SHALL be `networkRequestData`.

#### Scenario: Build session from NetworkRequestData

- **WHEN** `WebSocketSession.builder().networkRequestData(networkRequestData).build()` is called
- **THEN** a valid `WebSocketSession` is created with all transport operations delegated to the provided `NetworkRequestData`

### Requirement: Callers must not depend on Socket or SocketChannel

No code that consumes `WebSocketSession` SHALL import or reference `java.net.Socket` or `java.nio.channels.SocketChannel` for the purpose of I/O or lifecycle management on the session's connection. All such operations SHALL go through `NetworkRequestData`. This applies equally to blocking-path handlers and NIO-path handlers.

In particular, `NIOIncomingSocketHandler.routeToHandler()` SHALL NOT call `channel.socket().getOutputStream()` to construct an `OutputWrapper`. The `OutputWrapper` SHALL be constructed from a `NetworkRequestData`-backed output stream or replaced with `networkRequestData.write()` calls.

#### Scenario: Handler reads frames without socket reference

- **WHEN** a legacy handler processes a session
- **THEN** it reads data via `session.getNetworkRequestData().read()` and checks liveness via `session.getNetworkRequestData().isClosed()`

#### Scenario: NIO handler writes without channel reference

- **WHEN** `NIOIncomingSocketHandler` needs to write a handshake response
- **THEN** it calls `networkRequestData.write(bytes)` instead of constructing a `ByteBuffer` and calling `channel.write()`

#### Scenario: NIO handler routes without channel.socket()

- **WHEN** `NIOIncomingSocketHandler` routes to a `URIEndpointHandler`
- **THEN** the `OutputWrapper` is constructed via `OutputWrapper.from(networkRequestData)` — NOT via `channel.socket().getOutputStream()`

### Requirement: OutputWrapper accepts NetworkRequestData

`OutputWrapper` SHALL provide a static factory method `OutputWrapper.from(NetworkRequestData)` that internally creates an `OutputStream` backed by `networkRequestData.write(byte[])`. This eliminates the need for `channel.socket().getOutputStream()` without changing the `URIEndpointHandler` interface.

#### Scenario: Construct OutputWrapper from NetworkRequestData

- **WHEN** `OutputWrapper.from(networkRequestData)` is called
- **THEN** an `OutputWrapper` is created whose `outputStream()` writes bytes via `networkRequestData.write(byte[])`

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

### Requirement: ModernIncomingConnectionHandler interface removed

The `ModernIncomingConnectionHandler` interface SHALL be removed. `ConnectionState` SHALL call `handler.handle(context)` through the shared `IncomingConnectionHandler` interface. The previous `ModernIncomingConnectionHandler` was unnecessary indirection; `IncomingConnectionHandler` serves as the single shared entry point.

#### Scenario: ConnectionState calls handler through shared interface

- **WHEN** `ConnectionState.handle()` needs to invoke the handler
- **THEN** it calls `handler.handle(context)` on the `IncomingConnectionHandler` interface, without casting to `NIOIncomingSocketHandler`