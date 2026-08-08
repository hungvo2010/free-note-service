## Why

`NIOServerBootstrap.handleReadableEvent()` reacts to "data ready on channel" but never reads the data. It creates `NIONetworkRequestData(channel, emptyBuffer)` and passes it to the state machine, which calls `NIOIncomingSocketHandler.handShake()` — which then casts to `NIONetworkRequestData` and calls `readFromChannel()` + `prepareForRead()` itself. Two problems:

**1. I/O at the wrong abstraction level.** The selector loop owns the channel, the buffer lifecycle (line 115: `ByteBuffer.allocateDirect`), and the readiness notification — but the actual `channel.read()` happens two stack frames down in the handler. This splits buffer management across two classes: allocated in `NIOServerBootstrap`, filled in `NIOIncomingSocketHandler`. It also forces the handler to cast `NetworkRequestData` → `NIONetworkRequestData` on line 43, breaking the abstraction the parameter claims to provide.

**2. The shared `IncomingConnectionHandler` interface is bypassed.** `NIOIncomingSocketHandler.handle(ConnectionContext)` is dead code (`// unused`). The NIO path built a parallel API — `handShake()` and `handleInComingMessage()` — that `ConnectionState` calls directly on the concrete type. `NIOServerBootstrap.start()` takes `IncomingConnectionHandler` but immediately downcasts to `NIOIncomingSocketHandler` on line 40. The interface exists but nobody uses it.

The `nio-selector-abstraction` spec already requires: "Selector loop owns all raw I/O" and "NIOIncomingSocketHandler delegates I/O, does not perform it." The code doesn't match.

## What Changes

- **Move `readFromChannel()` + `prepareForRead()` + EOF check** from `NIOIncomingSocketHandler` into `NIOServerBootstrap.handleReadableEvent()` — the selector loop reads data into the buffer *before* dispatching, so the handler receives an already-filled `NIONetworkRequestData`
- **Route through `IncomingConnectionHandler.handle(ConnectionContext)`** — `ConnectionState` calls the shared interface method instead of NIO-specific `handShake()` / `handleInComingMessage()`. The `ConnectionContext` carries the `NetworkRequestData` (data already read) and enough context for the handler to know what to do
- **Remove `(NIONetworkRequestData)` casts** from the handler — after the move, the handler only calls interface methods: `read()`, `write()`, `getRemoteAddress()`, `isClosed()`
- **Remove the downcast in `NIOServerBootstrap.start()`** — the field and parameter both use `IncomingConnectionHandler`

## Capabilities

### New Capabilities

<!-- None — this change moves existing I/O calls to their correct owner and routes through the existing shared interface -->

### Modified Capabilities

- `nio-selector-abstraction`: The "Selector loop owns all raw I/O" requirement is enforced — `readFromChannel()`, `prepareForRead()`, and EOF handling move from handler to selector loop. The "NIOIncomingSocketHandler delegates I/O, does not perform it" requirement is enforced — `(NIONetworkRequestData)` casts and NIO-specific method calls removed from handler.
- `web-socket-session-abstraction`: `NIOIncomingSocketHandler.handle(ConnectionContext)` is the real entry point (no longer dead). `ConnectionState` calls the shared interface, not NIO-specific methods.

## Impact

- Affected code: `NIOServerBootstrap` (I/O moves up into `handleReadableEvent`), `NIOIncomingSocketHandler` (I/O removed, `handle()` becomes the real entry point), `ConnectionState` / `HandShakeState` / `ProcessingState` (call `handler.handle(context)` instead of `handler.handShake()`)
- No changes to `IncomingConnectionHandler` interface or blocking path
- No changes to endpoint handlers