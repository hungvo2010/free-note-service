## Context

The `NIOServerSession` class in `core.nio.events` has grown to mix transport-level I/O with connection lifecycle management. The per-connection state machine (`HandShakeState` → `MessageState`) and its mapping (`channelStates`) are embedded in the transport. `ConnectionState.handle(handler, context)` takes the handler as a parameter and calls it — a backwards dependency.

The branch `refactor/unify-interfaces-for-network-data` is part of a broader effort to make `NetworkRequestData` the single transport abstraction. This change continues that effort by separating the connection pipeline from the transport.

## Goals / Non-Goals

**Goals:**
- Move per-connection state management out of the transport layer
- Create a clear, testable pipeline that owns the lifecycle state machine
- Simplify `ConnectionState` to only decide transitions (no handler dependency)
- Fix all compilation errors in `NIOServerSession`
- Fix the span lifecycle leak
- Make the relationship between transport → pipeline → handler → state easy to understand

**Non-Goals:**
- Not adding NIO.2 support in this change (just making it possible)
- Not changing the `IncomingConnectionHandler` interface or `NIOIncomingSocketHandler`
- Not changing the `NetworkRequestData` interface
- Not removing `ReadableContext.networkRequestData` (optional, separate decision)

## Decisions

### 1. New class: `ConnectionPipeline`

A new class in `core.nio` that owns the per-connection lifecycle:
- `Map<NetworkRequestData, ConnectionState>` — keyed by `NetworkRequestData` (not `SocketChannel`), transport-agnostic
- Constructor takes `IncomingConnectionHandler` (the handler it will call)
- `process(NetworkRequestData)` → builds context, calls handler, transitions state, returns keepAlive
- `disconnect(NetworkRequestData)` → removes state on EOF or I/O error

Using `NetworkRequestData` as the map key (instead of `SocketChannel`) means the pipeline works with any transport — NIO, NIO.2, or blocking — as long as they provide a `NetworkRequestData` instance. `NIONetworkRequestData` already implements `hashCode()`/`equals()` from `Object`, which works for identity-based lookup (one instance per connection).

### 2. `ConnectionState` simplified to `transition(ConnectionContext)`

The handler parameter is removed. State implementations (`HandShakeState`, `MessageState`) no longer call `handler.handle()`. They only inspect `ConnectionContext` and decide the next state:
- `HandShakeState.transition()`: if `isHandshakeComplete()` → `MessageState`, else `this`
- `MessageState.transition()`: if `isClosed()` → `null`, else `this`

`throws IOException` is removed from the signature — transitions are pure inspection, no I/O.

### 3. `NIOServerSession` becomes a thin transport layer

Only transport concerns remain:
- `Map<SocketChannel, NIONetworkRequestData> channelData` — replaces the old `channelStates` + `channelBuffers` pair
- `acceptConnection()`: creates `NIONetworkRequestData(channel, buffer)`, stores in map, registers for OP_READ
- `handleReadEvent()`: reads bytes via `networkData.readFromChannel()` → `prepareForRead()` → `pipeline.process(networkData)` → cleanup if false
- `cleanupChannel()`: cancel selection key, close network data, decrement metric

Removed: `channelStates`, `channelBuffers`, dead `socketChannel` field, `buildStartSpan()`, all context-building imports.

### 4. Span lifecycle fixed

The current code has `span.end()` after the null check (line 105) but outside a `finally` block. If `state.handle()` throws, the span is never ended. The new pipeline wraps the handler call + transition in try/catch/finally:
```java
try {
    handler.handle(context);
    nextState = state.transition(context);
    ...
} catch (Exception e) {
    log, clean up, return false;
} finally {
    span.end();
}
```

### 5. Buffer lifecycle

In the new design, `ByteBuffer` is created in `acceptConnection()` and passed directly to `NIONetworkRequestData`. No separate `channelBuffers` map — `NIONetworkRequestData` owns its buffer for its lifetime. On cleanup, the `NIONetworkRequestData` is removed from `channelData` and becomes eligible for GC.

## Risks / Trade-offs

- **[Risk]** `NIONetworkRequestData` instances used as map keys must have stable identity. The current implementation uses default `Object.equals()`/`hashCode()`, which works because each connection gets exactly one instance.
- **[Mitigation]** This is already the pattern used in the old `channelStates` map — `SocketChannel` key identity was the lookup mechanism. Switching to `NetworkRequestData` key preserves this identity-based lookup.

- **[Risk]** The pipeline's `process` method does significant work (span creation, context building, handler invocation) on the selector thread.
- **[Mitigation]** This is unchanged from the current code — context building and handler invocation already happen on the selector thread. Moving it to the pipeline is a relocation, not a new threading concern.

## Entity Relationships (Post-Refactor)

```
NIOServerBootstrap
  ├── owns ──> NetworkSelector
  ├── owns ──> ServerSocketChannel
  ├── owns ──> ConnectionPipeline
  │              ├── uses ──> IncomingConnectionHandler
  │              └── owns ──> connectionStates: Map<NetworkRequestData, ConnectionState>
  │                              ├── HandShakeState
  │                              └── MessageState
  └── owns ──> NIOServerSession
                  └── owns ──> channelData: Map<SocketChannel, NIONetworkRequestData>
```

Data flow on read event:
```
NIOServerSession.handleReadEvent(NIOEvent)
  ├── channel = (SocketChannel) nioEvent.getChannel()
  ├── networkData = channelData.get(channel)
  ├── networkData.readFromChannel()           ← transport reads bytes
  ├── networkData.prepareForRead()
  ├── pipeline.process(networkData)            ← delegates to pipeline
  │     ├── state = connectionStates.computeIfAbsent(networkData, k -> new HandShakeState())
  │     ├── span = buildSpan(state)
  │     ├── Builds ReadableContext, ConnectionContext
  │     ├── handler.handle(context)           ← business logic
  │     ├── nextState = state.transition(context)  ← lifecycle decision
  │     └── Returns keepAlive (true/false)
  └── if !keepAlive → cleanupChannel()
```
