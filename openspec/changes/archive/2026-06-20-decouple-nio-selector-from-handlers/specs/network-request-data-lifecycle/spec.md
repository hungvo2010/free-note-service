## MODIFIED Requirements

### Requirement: Close connection
The `NetworkRequestData` interface SHALL provide a `void close() throws IOException` method that closes the underlying network connection. After calling `close()`, subsequent calls to `read()`, `write()`, and `buildRequestFrame()` SHALL throw `IOException`. In the NIO path, `close()` SHALL also cancel the associated `SelectionKey`.

#### Scenario: Close an open blocking connection
- **WHEN** `close()` is called on a `BlockingNetworkRequestData` with an open socket
- **THEN** the underlying `Socket` is closed and `isClosed()` returns `true`

#### Scenario: Close an open NIO connection
- **WHEN** `close()` is called on a `NIONetworkRequestData` with an open channel
- **THEN** the underlying `SocketChannel` is closed, the associated `SelectionKey` is cancelled, and `isClosed()` returns `true`

#### Scenario: Close an already-closed connection
- **WHEN** `close()` is called on a `NetworkRequestData` that is already closed
- **THEN** no exception is thrown (idempotent close)

### Requirement: Check if connection is closed
The `NetworkRequestData` interface SHALL provide a `boolean isClosed()` method that returns `true` if the underlying connection is no longer open. In the NIO path, this SHALL replace `channel.isOpen()` checks in handler code.

#### Scenario: Check open connection
- **WHEN** `isClosed()` is called on a `NetworkRequestData` with an active connection
- **THEN** it returns `false`

#### Scenario: Check closed connection
- **WHEN** `isClosed()` is called after `close()` has been invoked
- **THEN** it returns `true`

### Requirement: NIONetworkRequestData encapsulates ByteBuffer lifecycle
`NIONetworkRequestData` SHALL own the `ByteBuffer` lifecycle (`clear()`, `flip()`, `read`-into-buffer) that is currently performed by `NIOIncomingSocketHandler.emptyReadFromChannel()`. The handler SHALL NOT manipulate `ByteBuffer` directly.

#### Scenario: Prepare buffer for read
- **WHEN** the selector loop is about to read from a channel
- **THEN** it calls `nioNetworkRequestData.prepareForRead()` which handles `byteBuffer.clear()` or `byteBuffer.flip()` internally

#### Scenario: Read bytes from channel into buffer
- **WHEN** `nioNetworkRequestData.read(byteBuffer)` is called
- **THEN** it internally calls `channel.read(byteBuffer)` and returns the byte count
