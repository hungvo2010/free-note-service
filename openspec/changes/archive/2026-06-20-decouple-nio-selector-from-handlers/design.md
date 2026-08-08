## Context

The NIO path has a partially-decent architecture: `NIOServerBootstrap` already owns the selector loop (`select()` → dispatch → `SelectionKey` iteration), and delegates business work to `NIOIncomingSocketHandler` via a `ConnectionState` state machine. The architecture is:

```
Selector Loop (NIOServerBootstrap)
  → ConnectionState.handle(handler, ReadableContext)
    → NIOIncomingSocketHandler.handShake(ReadableContext)     // first read
    → NIOIncomingSocketHandler.handleInComingMessage(...)     // subsequent reads
```

The problem is **what leaks through `ReadableContext`**: raw `SocketChannel`, `SelectionKey`, and `ByteBuffer` are carried into the handler, which then performs its own I/O (`channel.read()`, `channel.write()`) instead of using `NetworkRequestData`. The structure is right; the abstractions are wrong.

## Goals / Non-Goals

**Goals:**
- Remove raw `SocketChannel`, `SelectionKey`, `ByteBuffer` from `ReadableContext` — replace with `NetworkRequestData`
- Remove all direct `channel.read()`, `channel.write()`, `ByteBuffer` manipulation from `NIOIncomingSocketHandler` — delegate to `NIONetworkRequestData`
- Remove `channel.socket().getOutputStream()` — use `NetworkRequestData`-based output
- Make `NIOIncomingSocketHandler` testable with a mock `NetworkRequestData` (same pattern used in the blocking path refactor)

**Non-Goals:**
- Restructure the `ConnectionState` / `HandShakeState` / `ProcessingState` state machine (deferred)
- Change the selector loop structure in `NIOServerBootstrap` (it already has the right shape)
- Merge `NIOIncomingSocketHandler` into the blocking path's `DefaultLegacySessionBasedConnectionHandler`
- Change endpoint handlers (`URIEndpointHandler` consumers)

## Integration Plans

### Handshake (first read after accept)

**Current — handler operates on raw NIO types:**

```
NIOServerBootstrap.handleReadableEvent()
  ├── key.attachment() → HandShakeState
  ├── ReadableContext(channel, key, byteBuffer, tracingContext)
  └── HandShakeState.handle(handler, context)
        └── handler.handShake(context)
              ├── emptyReadFromChannel(context.getChannel(), context.getByteBuffer())
              │     ├── byteBuffer.clear()
              │     ├── channel.read(byteBuffer)
              │     └── if -1 → channel.close()
              ├── httpParser.parse(byteBuffer)  → HttpUpgradeRequest
              └── performHandshake(channel, request)
                    ├── handshakeHandler.process(request)  → HttpUpgradeResponse
                    └── writeResponse(channel, responseBytes)
                          ├── ByteBuffer.wrap(data)
                          └── while hasRemaining → channel.write(buffer)
```

**After — handler delegates to NetworkRequestData:**

```
NIOServerBootstrap.handleReadableEvent()
  ├── key.attachment() → HandShakeState
  ├── ReadableContext(networkRequestData, tracingContext)   ← no raw types
  └── HandShakeState.handle(handler, context)
        └── handler.handShake(context)
              ├── context.getNetworkRequestData().read(bytes)   ← delegation
              │     └── NIONetworkRequestData: channel.read(buffer) + EOF → close
              ├── httpParser.parse(bytes)  → HttpUpgradeRequest
              └── performHandshake(request)
                    ├── handshakeHandler.process(request)  → HttpUpgradeResponse
                    └── context.getNetworkRequestData().write(responseBytes)
                          └── NIONetworkRequestData: ByteBuffer.wrap + channel.write loop
```

### Subsequent read (message processing)

**Current — handler mixes I/O and routing:**

```
NIOServerBootstrap.handleReadableEvent()
  ├── key.attachment() → ProcessingState
  ├── ReadableContext(channel, key, byteBuffer, tracingContext)
  └── ProcessingState.handle(handler, context)
        └── handler.handleInComingMessage(context, upgradeRequest)
              ├── emptyReadFromChannel(context.getChannel(), context.getByteBuffer())
              │     └── byteBuffer.clear() + channel.read() + EOF check
              └── routeToHandler(channel, byteBuffer, upgradeRequest)
                    ├── builtNetworkRequest(channel, byteBuffer)
                    │     └── new NIONetworkRequestData(channel, byteBuffer)
                    ├── new OutputWrapper(channel.socket().getOutputStream())  ← Demeter!
                    └── pathHandler.handle(networkRequestData, outputWrapper)
```

**After — handler delegates I/O, no raw channel access:**

```
NIOServerBootstrap.handleReadableEvent()
  ├── key.attachment() → ProcessingState
  ├── ReadableContext(networkRequestData, tracingContext)   ← same instance from handshake
  └── ProcessingState.handle(handler, context)
        └── handler.handleInComingMessage(context, upgradeRequest)
              ├── context.getNetworkRequestData().read(bytes)
              │     └── NIONetworkRequestData: internal channel.read + EOF → close
              └── routeToHandler(context, upgradeRequest)
                    ├── networkRequestData = context.getNetworkRequestData()
                    ├── outputWrapper = new OutputWrapper(networkRequestData.getOutputStream())
                    └── pathHandler.handle(networkRequestData, outputWrapper)
```

### Key differences

| Concern | Before | After |
| --- | --- | --- |
| Read operation | `channel.read(byteBuffer)` in handler | `networkRequestData.read(bytes)` — encapsulated |
| Write operation | `ByteBuffer.wrap()` + `channel.write()` loop in handler | `networkRequestData.write(bytes)` — encapsulated |
| OutputStream source | `channel.socket().getOutputStream()` | `networkRequestData.getOutputStream()` |
| Buffer lifecycle | `byteBuffer.clear()` in handler | inside `NIONetworkRequestData` |
| State management | `key.attach(state)` via `ReadableContext.setState()` | `NIOServerBootstrap` internal `Map<SocketChannel, State>` |
| Handler imports | `java.nio.ByteBuffer`, `java.nio.channels.SocketChannel` | none |

## Decisions

### Decision 1: Replace ReadableContext raw fields with NetworkRequestData

**Chosen**: `ReadableContext` carries `NetworkRequestData` + `TracingContext` only. Raw fields removed.

**Why**: The handler already receives a `NIONetworkRequestData` via `builtNetworkRequest()` for routing — but it also receives the raw channel separately. Having both is confusing and error-prone. `NetworkRequestData` already wraps the channel and buffer; `ReadableContext` should use it.

**How `SelectionKey` management moves**: Currently `ReadableContext.setState()` calls `key.attach(state)`. After the refactor, the selector loop holds a `Map<SocketChannel, ProcessingState>` internally, or the state remains on the key attachment but is accessed only by `NIOServerBootstrap` (not by the handler).

### Decision 2: Encapsulate emptyReadFromChannel into NIONetworkRequestData

**Chosen**: Move the read + EOF check into `NIONetworkRequestData`. The handler calls `networkRequestData.readIntoBuffer()` which returns the byte count.

**Why**: The pattern `byteBuffer.clear(); channel.read(byteBuffer); if == -1 close()` appears in `NIOIncomingSocketHandler.emptyReadFromChannel()` and is called from both `handShake()` and `handleInComingMessage()`. This is pure NIO I/O — it belongs in the `NetworkRequestData` implementation, not the handler.

### Decision 3: Eliminate channel.socket().getOutputStream()

**Chosen**: `NIONetworkRequestData` gains a `getOutputStream()` method, or the handler switches to `networkRequestData.write(byte[])` for all writes.

**Why**: `channel.socket().getOutputStream()` is a Law of Demeter violation (3 levels deep: channel → socket → output stream) and ties the handler to blocking I/O even though it's in the NIO path. `NIONetworkRequestData` already has `write(byte[])` which wraps data in a `ByteBuffer` and calls `channel.write()` in a loop — this should be used for all writes.

**Alternative considered**: Add `OutputStream getOutputStream()` to `NetworkRequestData` interface. Rejected — the `write(byte[])` method is simpler and doesn't expose `OutputStream` semantics to callers. `OutputWrapper` can be constructed from a `NetworkRequestData`-backed stream if needed, or the endpoint handler interface can be adapted.

### Decision 4: Keep NIOServerBootstrap selector loop as-is

**Chosen**: No structural change to `NIOServerBootstrap`. It already has the right Reactor pattern — the selector loop dispatches to `ConnectionState` which invokes the handler.

**Why**: The selector loop is correctly placed in `NIOServerBootstrap`. The fix is at the boundary — what data the handler receives and what I/O primitives it uses — not in the loop structure itself.

## Risks / Trade-offs

- **[NIONetworkRequestData existing behavior]**: `NIONetworkRequestData.read(byte[])` currently returns 0 (no-op). It must be enhanced to actually read from the channel. → Update `NIONetworkRequestData` to accept a `ByteBuffer` and perform the read internally.
- **[State machine coupling]**: `ConnectionState` / `HandShakeState` / `ProcessingState` still use `ReadableContext` — these must be updated for the new field types. → Part of the implementation tasks; the state classes are NIO-internal and only invoked from `NIOServerBootstrap`.
- **[OutputWrapper dependency]**: `URIEndpointHandler.handle()` takes `OutputWrapper(OutputStream)`. If we eliminate `channel.socket().getOutputStream()`, we need another way to construct an `OutputStream` from `NetworkRequestData`. → `NIONetworkRequestData` can provide an internal `OutputStream` backed by `write(byte[])`, or `OutputWrapper` can be adapted to accept `NetworkRequestData` directly.
- **[Performance]**: Moving `channel.read()` inside `NIONetworkRequestData` adds a method call indirection. → Negligible — the JIT compiler inlines such calls. The blocking path already does this without issue.

## Open Questions

- Should `OutputWrapper` be refactored to accept `NetworkRequestData` instead of (or in addition to) `OutputStream`?
- Should `NIONetworkRequestData` expose a `ByteBuffer` accessor for the selector loop, or should the selector loop manage its own buffer pool?
- Can `ConnectionState` be simplified once `ReadableContext` no longer carries raw channel/key/buffer?
