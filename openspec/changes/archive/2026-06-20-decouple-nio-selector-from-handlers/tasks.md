## 1. Enhance NIONetworkRequestData

- [x] 1.1 Update `NIONetworkRequestData.read(byte[])` to actually read from `SocketChannel` into the provided byte array
- [x] 1.2 Add `prepareForRead()` method (or make existing one handle buffer lifecycle fully)
- [x] 1.3 Add `OutputStream getOutputStream()` or ensure `write(byte[])` covers all write use cases currently done via `channel.socket().getOutputStream()`

## 2. Refactor ReadableContext

- [x] 2.1 Add `private final NetworkRequestData networkRequestData` field
- [x] 2.2 Remove `private SocketChannel channel` field — delegate to `networkRequestData`
- [x] 2.3 Remove `private SelectionKey key` field — move state management to `NIOServerBootstrap` internal map
- [x] 2.4 Remove `private ByteBuffer byteBuffer` field — move buffer lifecycle to `NIONetworkRequestData`
- [x] 2.5 Rewrite `closeChannel()` → delegate to `networkRequestData.close()`
- [x] 2.6 Rewrite `getRemoteAddress()` → delegate to `networkRequestData.getRemoteAddress()`
- [x] 2.7 Remove `setState(ProcessingState)` — state management moves to selector loop

## 3. Clean up NIOIncomingSocketHandler

- [x] 3.1 Remove `emptyReadFromChannel(SocketChannel, ByteBuffer)` method — replace with `networkRequestData.read(byte[])` calls
- [x] 3.2 Remove `writeResponse(SocketChannel, byte[])` method — replace with `networkRequestData.write(byte[])`
- [x] 3.3 Remove `channel.socket().getOutputStream()` in `routeToHandler()` — construct `OutputWrapper` from `NetworkRequestData`-backed stream
- [x] 3.4 Remove `java.nio.ByteBuffer` import
- [x] 3.5 Remove `java.nio.channels.SocketChannel` import
- [x] 3.6 Update `handShake(ReadableContext)` to use new `ReadableContext` API
- [x] 3.7 Update `handleInComingMessage(ReadableContext, ...)` to use new `ReadableContext` API

## 4. Update NIOServerBootstrap

- [x] 4.1 Add internal `Map<SocketChannel, ProcessingState>` for state management (replacing `key.attach()`)
- [x] 4.2 Update `handleReadableEvent()` to construct `ReadableContext` with `NetworkRequestData` instead of raw fields
- [x] 4.3 Update `handleNewConnectionEvent()` — create `NIONetworkRequestData` at accept time and store in internal map
- [x] 4.4 Ensure `SelectionKey.cancel()` is called by `NIONetworkRequestData.close()`

## 5. Update ConnectionState classes

- [x] 5.1 Update `ConnectionState` / `HandShakeState` / `ProcessingState` to work with the new `ReadableContext` API
- [x] 5.2 Remove any direct `channel` or `byteBuffer` access from state classes

## 6. Verify and test

- [x] 6.1 Run `./gradlew compileJava` — ensure no compilation errors
- [x] 6.2 Verify no `java.nio.channels.SocketChannel` imports in handler code (outside `NIONetworkRequestData` and `NIOServerBootstrap`)
- [x] 6.3 Verify no `java.nio.ByteBuffer` imports in handler code (outside `NIONetworkRequestData` and `NIOServerBootstrap`)
- [x] 6.4 Verify no `channel.socket().getOutputStream()` remains anywhere in the codebase
- [x] 6.5 Run tests — ensure no regressions

## 7. Sync and finalize

- [x] 7.1 Sync delta specs to main specs
- [x] 7.2 Update launchers if `NIOWebSocketServer` construction changes
