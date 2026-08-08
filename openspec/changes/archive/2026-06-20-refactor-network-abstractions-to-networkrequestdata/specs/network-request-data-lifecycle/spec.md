## ADDED Requirements

### Requirement: Close connection
The `NetworkRequestData` interface SHALL provide a `void close() throws IOException` method that closes the underlying network connection. After calling `close()`, subsequent calls to `read()`, `write()`, and `buildRequestFrame()` SHALL throw `IOException`.

#### Scenario: Close an open blocking connection
- **WHEN** `close()` is called on a `BlockingNetworkRequestData` with an open socket
- **THEN** the underlying `Socket` is closed and `isClosed()` returns `true`

#### Scenario: Close an open NIO connection
- **WHEN** `close()` is called on a `NIONetworkRequestData` with an open channel
- **THEN** the underlying `SocketChannel` is closed and `isClosed()` returns `true`

#### Scenario: Close an already-closed connection
- **WHEN** `close()` is called on a `NetworkRequestData` that is already closed
- **THEN** no exception is thrown (idempotent close)

### Requirement: Check if connection is closed
The `NetworkRequestData` interface SHALL provide a `boolean isClosed()` method that returns `true` if the underlying connection is no longer open.

#### Scenario: Check open connection
- **WHEN** `isClosed()` is called on a `NetworkRequestData` with an active connection
- **THEN** it returns `false`

#### Scenario: Check closed connection
- **WHEN** `isClosed()` is called after `close()` has been invoked
- **THEN** it returns `true`

### Requirement: Get remote address
The `NetworkRequestData` interface SHALL provide an `Object getRemoteAddress()` method that returns the remote address of the connected client, or `null` if the connection is not established or the address is unavailable.

#### Scenario: Get remote address from blocking connection
- **WHEN** `getRemoteAddress()` is called on a `BlockingNetworkRequestData` wrapping an accepted socket
- **THEN** it returns the remote `SocketAddress` from the underlying `Socket`

#### Scenario: Get remote address from NIO connection
- **WHEN** `getRemoteAddress()` is called on a `NIONetworkRequestData` wrapping an active channel
- **THEN** it returns the remote `SocketAddress` from the underlying `SocketChannel`

#### Scenario: Get remote address when unavailable
- **WHEN** `getRemoteAddress()` is called and the underlying transport cannot determine the address
- **THEN** it returns `null`