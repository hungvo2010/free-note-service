## 1. Simplify `ConnectionState` interface

- [ ] 1.1 Change `ConnectionState.java`: rename `handle(IncomingConnectionHandler, ConnectionContext) throws IOException` to `transition(ConnectionContext)`
- [ ] 1.2 Remove `IncomingConnectionHandler` import from `ConnectionState.java`
- [ ] 1.3 Update `HandShakeState.java`: rename `handle()` → `transition()`, remove handler parameter, remove `handler.handle(context)` call, remove try/catch (moves to processor), remove `IncomingConnectionHandler` import
- [ ] 1.4 Update `MessageState.java`: rename `handle()` → `transition()`, remove handler parameter, remove `handler.handle(context)` call, remove catch blocks (moves to processor), remove `IncomingConnectionHandler` and `ClientDisconnectException` imports

## 2. Create `ConnectionPipeline`

- [ ] 2.1 Create `ConnectionPipeline.java` in `com.freenote.app.server.core.nio`
- [ ] 2.2 Add `Map<NetworkRequestData, ConnectionState> connectionStates` (ConcurrentHashMap)
- [ ] 2.3 Add `IncomingConnectionHandler handler` field (constructor injection)
- [ ] 2.4 Implement `boolean process(NetworkRequestData networkData)`:
  - Lazily create `HandShakeState` via `computeIfAbsent`
  - Build span with `buildSpan(state)`
  - Build `TracingContext`, `ReadableContext`, `ConnectionContext`
  - Call `handler.handle(context)` in try block
  - Call `state.transition(context)` for lifecycle decision
  - Return `false` on exception or null transition, `true` otherwise
  - `span.end()` in `finally` block
- [ ] 2.5 Implement `void disconnect(NetworkRequestData networkData)` — remove state
- [ ] 2.6 Implement `Span buildSpan(ConnectionState state)` — moved from `NIOServerSession`

## 3. Refactor `NIOServerSession` (transport only)

- [ ] 3.1 Remove fields: `channelStates`, `channelBuffers`, dead `socketChannel`
- [ ] 3.2 Add fields: `ConnectionPipeline pipeline`, `Map<SocketChannel, NIONetworkRequestData> channelData`
- [ ] 3.3 Remove unused imports: `ConnectionState`, `HandShakeState`, `MessageState`, `ReadableContext`, `ConnectionContext`, `TracingContext`, `Span`, `IncomingConnectionHandler`
- [ ] 3.4 Simplify `acceptConnection()`: create `NIONetworkRequestData(channel, buffer)`, store in `channelData`
- [ ] 3.5 Simplify `handleReadEvent()`: get `NIONetworkRequestData` from `channelData`, read bytes, call `pipeline.process()`, cleanup if false
- [ ] 3.6 Update `readChannelData()`: call `pipeline.disconnect()` before cleanup on error/EOF
- [ ] 3.7 Update `cleanupChannel()`: use `nioEvent.getSelectionKey()` for key cancel, use `networkData` for close (fix undefined `channel`/`key` bugs)
- [ ] 3.8 Remove `buildStartSpan()` method (moved to pipeline)

## 4. Wire in `NIOServerBootstrap`

- [ ] 4.1 Remove `private IncomingConnectionHandler handler` field (was stored but never read)
- [ ] 4.2 Create `ConnectionPipeline` with handler before building session
- [ ] 4.3 Pass pipeline via builder: `NIOServerSession.builder()...pipeline(connectionPipeline).build()`

## 5. Optional: Clean up `ReadableContext`

- [ ] 5.1 Remove `private final NetworkRequestData networkRequestData` field
- [ ] 5.2 Remove `NetworkRequestData` import
- [ ] 5.3 Remove dead `// NOTE: only use NIO` comment

## 6. Verification

- [ ] 6.1 `mvn compile` — must pass clean
- [ ] 6.2 `mvn test` — verify no regressions
- [ ] 6.3 Manual trace: verify read-event flow end-to-end (selector → session → processor → handler → state)
