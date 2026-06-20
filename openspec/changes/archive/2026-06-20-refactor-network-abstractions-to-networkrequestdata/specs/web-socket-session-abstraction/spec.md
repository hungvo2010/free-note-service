## ADDED Requirements

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
No code that consumes `WebSocketSession` SHALL import or reference `java.net.Socket` or `java.nio.channels.SocketChannel` for the purpose of I/O or lifecycle management on the session's connection. All such operations SHALL go through `NetworkRequestData`.

#### Scenario: Handler reads frames without socket reference
- **WHEN** a legacy handler processes a session
- **THEN** it reads data via `session.getNetworkRequestData().read()` and checks liveness via `session.getNetworkRequestData().isClosed()`
