## Context

`NIOServerBootstrap.handleReadableEvent()` creates `NIONetworkRequestData(channel, emptyBuffer)` and dispatches to the state machine — which calls `NIOIncomingSocketHandler.handShake()`, which casts back to `NIONetworkRequestData` and calls `readFromChannel()` + `prepareForRead()`. The I/O happens in the handler, not the selector loop. Meanwhile, the shared `IncomingConnectionHandler.handle(ConnectionContext)` interface sits unused (`// unused`).

This design moves I/O to its correct owner and routes dispatch through the shared interface.

## Goals / Non-Goals

**Goals:**
- `NIOServerBootstrap.handleReadableEvent()` performs `readFromChannel()` + `prepareForRead()` + EOF check before dispatch — the selector loop owns all raw I/O
- `NIOIncomingSocketHandler` no longer casts `NetworkRequestData` to `NIONetworkRequestData` — the handler only calls interface methods (`read()`, `write()`, `getRemoteAddress()`, `isClosed()`)
- `ConnectionState.handle()` calls `handler.handle(ConnectionContext)` — the shared `IncomingConnectionHandler` interface, not NIO-specific methods
- `NIOServerBootstrap.start()` takes `IncomingConnectionHandler` without downcast

**Non-Goals:**
- Changing the `IncomingConnectionHandler` or `ServerBootstrap` interfaces
- Changing endpoint handlers (`URIEndpointHandler`)
- Changing the `ConnectionState` return-type contract (`ConnectionState handle(...)` — stays)
- Merging `HandShakeState` / `ProcessingState` into one state
- Removing `AcceptHandshakeHandler` / `HttpParser` interfaces from the handler

## Decisions

### Decision 1: I/O moves to `handleReadableEvent()`, before dispatch

`readFromChannel()`, `prepareForRead()`, and the EOF → close path move from `NIOIncomingSocketHandler.handShake()` / `handleInComingMessage()` into `NIOServerBootstrap.handleReadableEvent()`, immediately after the `NIONetworkRequestData` is constructed (line 143) and before the tracing span or state dispatch.

**Why**: The selector loop already owns the channel, the buffer lifecycle (allocation at accept, disposal at close), and the readiness notification. Having the actual `channel.read()` happen in the handler splits buffer management across two classes and forces the handler to know about NIO internals. After the move, the handler receives a `NIONetworkRequestData` that already has data in its buffer — ready for `read()`.

**Alternative**: Leave I/O in the handler but have `handleReadableEvent()` call `networkData.readFromChannel()` and pass the already-filled buffer to the handler. Rejected — same result but the handler would still receive a `NIONetworkRequestData` it needs to cast for non-I/O operations.

### Decision 2: `ConnectionState.handle()` calls `IncomingConnectionHandler.handle(ConnectionContext)`

The `ConnectionState` interface changes from:
```java
ConnectionState handle(NIOIncomingSocketHandler handler, NetworkRequestData networkData)
```
to:
```java
ConnectionState handle(IncomingConnectionHandler handler, ConnectionContext context)
```

Both `HandShakeState` and `ProcessingState` call `handler.handle(context)` — the shared interface method.

**Why**: `IncomingConnectionHandler` is the shared abstraction between blocking and NIO paths. The NIO path currently bypasses it with a parallel API (`handShake()` / `handleInComingMessage()`). Routing through `handle(ConnectionContext)` makes the interface real for the NIO path.

**Alternative**: Keep `ConnectionState` calling NIO-specific methods and call `handle()` from another place. Rejected — `ConnectionState` is the dispatch point; if it doesn't call the interface, nothing will.

### Decision 3: `ReadableContext` carries `HttpUpgradeRequest` for state transitions

`ReadableContext` gains a mutable `httpUpgradeRequest` field (set after construction). During handshake, `NIOIncomingSocketHandler.handle()` parses the HTTP upgrade request and stores it via `readableContext.setHttpUpgradeRequest(request)`. `HandShakeState` reads it after `handle()` returns to determine the state transition.

**Why**: `IncomingConnectionHandler.handle()` returns `void`. The state machine needs the parsed `HttpUpgradeRequest` to construct `ProcessingState`. Storing it on the context lets the handler communicate results back to the state machine without changing the shared interface.

**Alternative A**: Have `handle()` return `HttpUpgradeRequest`. Rejected — changes the shared `IncomingConnectionHandler` interface which returns `void` for the blocking path.

**Alternative B**: Store the request in a separate map in `NIOServerBootstrap`. Rejected — adds complexity; `ReadableContext` is already carried through the dispatch and is the natural place for per-event state.

### Decision 4: `NIOServerBootstrap.start()` keeps `IncomingConnectionHandler` parameter

The `start()` method parameter type stays `IncomingConnectionHandler`. The downcast `(NIOIncomingSocketHandler) handler` on line 40 is removed. The field type changes from `NIOIncomingSocketHandler` to `IncomingConnectionHandler`.

**Why**: The `ServerBootstrap` interface defines `start(IncomingConnectionHandler, ServerSocketConfig)`. The NIO path should honor the interface, not work around it. Since `ConnectionState` now calls `handler.handle(context)` through the interface, `NIOServerBootstrap` never needs the concrete type.

## Risks / Trade-offs

- **[Handler internal dispatch]**: `handle()` must internally decide "am I doing handshake or processing a message?" It uses `ReadableContext.isHandshakeComplete()` as the discriminator. If this state gets out of sync (e.g., the request is cleared but the state machine thinks we're still processing), the handler would incorrectly re-handshake. → Mitigation: `NIOServerBootstrap` manages state transitions deterministically; the handler only reads, never clears, the stored request.
- **[Mutable context]**: Adding a mutable field to `ReadableContext` breaks its current immutability. → Mitigation: Only the `httpUpgradeRequest` field is settable; the `networkRequestData` and `tracingContext` remain final. The field is set once during handshake and never mutated afterward.
- **[Tracing]**: Currently `handleReadableEvent()` creates a span AND `handleInComingMessage()` creates a nested span. After the refactor, `handle()` is called for both handshake and message processing — span management should be consolidated. → The outer span stays in `handleReadableEvent()`; the inner span in `handleInComingMessage()` is removed (it was duplicative).

## Open Questions

- Should the inner tracing span (`buildMessageSpan`) be preserved? It adds `origin`, `path`, `uri` attributes not on the outer span. Could be merged into the outer span instead.
- Should `HandShakeState` and `ProcessingState` be merged into a single state now that both call `handler.handle(context)`? Deferred for a follow-up change.
