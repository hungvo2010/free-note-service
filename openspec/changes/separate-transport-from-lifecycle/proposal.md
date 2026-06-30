## Why

`NIOServerSession` currently mixes two concerns:
1. **Transport** — reading/writing bytes, selector registration, channel lifecycle
2. **Connection lifecycle** — per-channel state machine (`HandShakeState` → `MessageState` → close)

This coupling means adding a new transport (NIO.2 `AsynchronousSocketChannel`, Netty, or even blocking IO) requires reimplementing the entire connection state machine. Additionally, `ConnectionState.handle(handler, context)` has a backwards dependency where state calls the handler — the relationship is unclear and difficult to explain.

The current code also has several compilation errors (`handler` undefined, `key` undefined, `channel` undefined in `cleanupChannel`) and a span leak (span not ended in a `finally` block).

## What Changes

- **`ConnectionState` interface**: `handle(handler, context)` → `transition(context)` — state only decides lifecycle transitions, no longer calls the handler
- **New `ConnectionPipeline`**: owns per-connection state map, calls `handler.handle()` then `state.transition()`, manages span lifecycle
- **`NIOServerSession` simplified**: removes `channelStates`/`channelBuffers` maps, removes dead `socketChannel` field, becomes a thin I/O layer that reads bytes and delegates to the pipeline
- **`NIOServerBootstrap` wiring**: creates pipeline, passes it to session via builder
- **`ReadableContext` cleanup** (optional): removes unused `networkRequestData` field (zero usages)

## Capabilities

### New Capabilities
- `connection-pipeline`: A transport-agnostic pipeline that owns connection lifecycle state, orchestrates handler invocation and state transitions, and manages tracing spans

### Modified Capabilities
- `nio-selector-abstraction`: ConnectionState no longer takes handler parameter; NIOServerSession (not NIOServerBootstrap) owns channel-to-data mapping; ConnectionPipeline owns state mapping
- `readable-context-refactor`: Removes unused `networkRequestData` field (optional — can be deferred)

## Impact

- **Transport pluggability**: Adding NIO.2 requires only implementing a thin transport adapter — the pipeline, handler, and state machine are reused as-is
- **Code clarity**: Clear direction — transport reads bytes → pipeline calls handler → state decides transition → transport cleans up
- **Bug fixes**: All compilation errors in `NIOServerSession` resolved; span lifecycle correctly managed with try/finally
