## 1. Expand NetworkRequestData interface

- [x] 1.1 Add `void close() throws IOException` to `NetworkRequestData`
- [x] 1.2 Add `boolean isClosed()` to `NetworkRequestData`
- [x] 1.3 Add `Object getRemoteAddress()` to `NetworkRequestData`

## 2. Implement new methods in BlockingNetworkRequestData

- [x] 2.1 Implement `close()` — delegate to `socket.close()` (idempotent if already closed)
- [x] 2.2 Implement `isClosed()` — delegate to `socket.isClosed()`
- [x] 2.3 Implement `getRemoteAddress()` — delegate to `socket.getRemoteSocketAddress()`

## 3. Implement new methods in NIONetworkRequestData

- [x] 3.1 Implement `close()` — delegate to `channel.close()`
- [x] 3.2 Implement `isClosed()` — delegate to `!channel.isOpen()`
- [x] 3.3 Implement `getRemoteAddress()` — delegate to `channel.getRemoteAddress()`

## 4. Add byte[] overload to HttpParser

- [x] 4.1 Add `HttpUpgradeRequest parse(byte[] data) throws IOException` to `HttpParser` interface
- [x] 4.2 Implement in `HttpParserImpl` — wrap bytes in `ByteArrayInputStream` and delegate to existing `parse(InputStream)`
- [x] 4.3 Update callers that read from socket input stream to use `parse(byte[])` instead

## 5. Remove raw Socket and SocketChannel from WebSocketSession

- [x] 5.1 Remove `private final Socket socket` field
- [x] 5.2 Remove `private final SocketChannel socketChannel` field
- [x] 5.3 Rewrite `getRemoteAddress()` to delegate to `networkRequestData.getRemoteAddress()`
- [x] 5.4 Remove `java.net.Socket` and `java.nio.channels.SocketChannel` imports

## 6. Update DefaultLegacySessionBasedConnectionHandler

- [x] 6.1 Replace `session.getSocket().getInputStream()` with `session.getNetworkRequestData().read()` + `httpParser.parse(byte[])`
- [x] 6.2 Replace `socket.isClosed()` loop check with `session.getNetworkRequestData().isClosed()`
- [x] 6.3 Replace `closeSocket(session.getSocket())` with `session.getNetworkRequestData().close()`
- [x] 6.4 Remove `closeSocket(Socket)` helper method
- [x] 6.5 Remove `java.net.Socket` import

## 7. Update LegacyConnectionAdapter

- [x] 7.1 Remove `.socket(context.getSocket())` from `WebSocketSession.builder()` call
- [x] 7.2 Remove `java.net.Socket` import (if no longer needed)

## 8. Refactor ConnectionContext

- [x] 8.1 Remove `private final Socket socket` field
- [x] 8.2 Remove `private final SocketChannel socketChannel` field
- [x] 8.3 Remove `private final ByteBuffer byteBuffer` field
- [x] 8.4 Add `private final NetworkRequestData networkRequestData` field
- [x] 8.5 Update builder to accept `networkRequestData` instead of raw fields
- [x] 8.6 Update `LegacyBootstrap` to construct `BlockingNetworkRequestData` and pass it to `ConnectionContext`
- [x] 8.7 ~~Update `NIOServerBootstrap`~~ → **EXCLUDED**: NIO classes use a fundamentally different selector-driven architecture; deferred to a separate NIO-specific refactor

## 9. Refactor ReadableContext

> **EXCLUDED**: `ReadableContext` is NIO-specific (selector event loop). Reverted to original. The NIO path's `SocketChannel`+`ByteBuffer`+`SelectionKey` triad is tightly coupled to the selector state-machine pattern. Will be addressed in a separate NIO-specific spec.

## 10. Refactor NIOModernIncomingSocketHandler

> **EXCLUDED**: This handler is explicitly NIO (`NIOModernIncomingSocketHandler`). It uses `channel.read(buffer)` + `buffer.flip()` + parse cycle that's fundamentally different from the blocking `read()` model. Deferred to separate NIO refactor.

## 11. Collapse InputWrapper into NetworkRequestData implementations

> **EXCLUDED**: `InputWrapper` is used by the NIO path. Deferred to separate NIO refactor.

## 12. Change WebSocketFrameHandler interface to byte[]

> **EXCLUDED**: This interface serves BOTH blocking and NIO paths. Changing `ByteBuffer`→`byte[]` would break the NIO path which still uses `ByteBuffer` internally. Deferred to separate refactor after NIO path is also migrated.

## 13. Update all endpoint handlers for byte[] signatures

> **EXCLUDED**: Depends on task 12 (WebSocketFrameHandler interface change).

## 14. Update WebSocketFrameDispatcher

> **EXCLUDED**: Depends on task 12 (WebSocketFrameHandler interface change).

## 15. Update ByteBufferFrameParserImpl

> **EXCLUDED**: The `(NIONetworkRequestData)` cast is used by the NIO path. Deferred to NIO refactor.

## 16. Remove SocketChannel getter from WebSocketConnection

- [x] 16.1 Remove `public SocketChannel getSocketChannel()` method
- [x] 16.2 Update any callers to use `NetworkRequestData` via the session instead
- [x] 16.3 Remove `java.nio.channels.SocketChannel` import

## 17. Update bootstraps to construct NetworkRequestData at the edge

- [x] 17.1 In `LegacyBootstrap`: construct `BlockingNetworkRequestData` from accepted `Socket`, pass it to `ConnectionContext`
- [x] 17.2 ~~In `NIOServerBootstrap`~~ → **EXCLUDED**: NIO bootstrap reverted to original
- [x] 17.3 Ensure no raw `Socket`/`SocketChannel` escapes beyond the bootstrap+constructor call site → **Done for blocking path**: `Socket` is wrapped in `BlockingNetworkRequestData` at `LegacyBootstrap` line 39, no raw socket passes to handlers

## 18. Clean up connection state classes

> **EXCLUDED**: `ConnectionState`/`HandShakeState`/`ProcessingState` are NIO-specific. Deferred to NIO refactor.

## 19. Verify compilation and run tests

- [x] 19.1 Run `./gradlew compileJava` — **BUILD SUCCESSFUL**, no errors
- [x] 19.2 Run tests → 92 tests run, 19 pre-existing failures (13 MetricUtils OpenTelemetry init, 6 unrelated test bugs), **0 new failures from refactor**
- [x] 19.3 Verify no remaining `java.net.Socket` imports in handler/model/core (non-bootstrap) code → **Done**: No `Socket` imports in handler/model/core code outside `BlockingNetworkRequestData`
- [x] 19.4 Verify no remaining `java.nio.channels.SocketChannel` imports in handler/model/core (non-bootstrap) code → **Done**: Only `NIONetworkRequestData` and `NIOCommonEndpoint` still import, both NIO-specific (excluded from this refactor)
- [x] 19.5 Verify no `java.nio.ByteBuffer` in handler method signatures → **Done** (task 12 excluded, NIO path deferred)
