## 1. Update ReadableContext

- [x] 1.1 Add `HttpUpgradeRequest httpUpgradeRequest` field with `@Setter` (mutable, set once after handshake)
- [x] 1.2 Add `isHandshakeComplete()` convenience method encapsulating the handshake state check

## 2. Move I/O into NIOServerBootstrap.handleReadableEvent()

- [x] 2.1 After `NIONetworkRequestData` construction, call `networkData.readFromChannel()` — if -1, close, clean up state/buffer maps, cancel key, decrement metrics, return
- [x] 2.2 Call `networkData.prepareForRead()` after successful read
- [x] 2.3 Build `ConnectionContext` with the filled `networkRequestData` + `ReadableContext` (with `TracingContext`) before dispatch
- [x] 2.4 Pass `ConnectionContext` (not raw `NetworkRequestData`) to `state.handle()`
- [x] 2.5 Remove `MetricUtils.decrementConcurrentUsers()` from EOF path in handler (moved up)

## 3. Update ConnectionState interface and implementations

- [x] 3.1 Change `ConnectionState.handle()` signature: `ConnectionState handle(IncomingConnectionHandler handler, ConnectionContext context) throws IOException`
- [x] 3.2 Update `HandShakeState.handle()` — call `handler.handle(context)`, then read `context.getReadableContext().isHandshakeComplete()` to decide transition to `ProcessingState` or return `this`
- [x] 3.3 Update `ProcessingState.handle()` — call `handler.handle(context)`, then check `context.getNetworkRequestData().isClosed()` to return `null` (close) or `this` (stay)
- [x] 3.4 `ProcessingState` constructor keeps `HttpUpgradeRequest` — stored on `ReadableContext` by handler, constructor receives it from context

## 4. Refactor NIOIncomingSocketHandler

- [x] 4.1 Remove `readFromChannel()` + `prepareForRead()` + EOF check from `handShake()` method — data is already in buffer
- [x] 4.2 Remove `readFromChannel()` + `prepareForRead()` + EOF check from `handleInComingMessage()` method — data is already in buffer
- [x] 4.3 Remove all `(NIONetworkRequestData)` casts — only call `NetworkRequestData` interface methods
- [x] 4.4 Implement `handle(ConnectionContext)` as the real entry point — if `context.getReadableContext().isHandshakeComplete()` is false, do handshake and store result; otherwise route to handler with stored request
- [x] 4.5 Remove duplicate inner tracing span from `handleInComingMessage()` (outer span already created in `handleReadableEvent()`)
- [x] 4.6 Remove `NIONetworkRequestData` import (no longer cast or referenced directly)

## 5. Update NIOServerBootstrap field type

- [x] 5.1 Change `handler` field type from `NIOIncomingSocketHandler` to `IncomingConnectionHandler`
- [x] 5.2 Remove `(NIOIncomingSocketHandler)` cast on line 40 — assign parameter directly
- [x] 5.3 Remove `NIOIncomingSocketHandler` import (no longer referenced)

## 6. Verify and test

- [x] 6.1 Run `./gradlew compileJava` — ensure no compilation errors
- [x] 6.2 Verify zero `(NIONetworkRequestData)` casts in `NIOIncomingSocketHandler`
- [x] 6.3 Verify zero `readFromChannel()` / `prepareForRead()` calls in `NIOIncomingSocketHandler`
- [x] 6.4 Verify `handle(ConnectionContext)` is not dead code — called by both `HandShakeState` and `ProcessingState`
- [x] 6.5 Run `./gradlew test` — ensure no regressions
