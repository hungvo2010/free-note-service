## Context

The codebase already defines `NetworkRequestData` as the core abstraction for network I/O, with two implementations: `BlockingNetworkRequestData` (wraps `Socket`) and `NIONetworkRequestData` (wraps `SocketChannel`/`ByteBuffer`). However, the abstraction is incomplete — 19+ files still import and operate on raw `java.net.Socket`, `java.nio.channels.SocketChannel`, `java.nio.ByteBuffer`, and `java.io.InputStream`/`OutputStream`. Domain objects like `ConnectionContext`, `ReadableContext`, and `WebSocketSession` hold these raw types as fields, leaking transport details to every layer of the application. The `WebSocketFrameHandler` interface passes `ByteBuffer` to endpoint handlers, coupling business logic to NIO buffer semantics. Bootstrap code constructs raw sockets/channels and threads them through builders to leaf consumers.

The goal is to make `NetworkRequestData` the **single surface** for network I/O — constructed at the bootstrap edge, passed through domain objects, and consumed by handlers without any layer knowing the underlying transport type.

## Goals / Non-Goals

**Goals:**
- Eliminate all `java.net.Socket` references from code above the `NetworkRequestData` implementation layer
- Eliminate all `java.nio.channels.SocketChannel` references from code above the `NetworkRequestData` implementation layer
- Replace `java.nio.ByteBuffer` with `byte[]` in handler interfaces so business logic doesn't depend on NIO types
- Expand `NetworkRequestData` with lifecycle methods (`close()`, `isClosed()`) and metadata (`getRemoteAddress()`) so callers never need the transport reference for any reason
- Make `ConnectionContext` and `ReadableContext` carry `NetworkRequestData` instead of raw transport fields
- Remove the `socket` and `socketChannel` fields from `WebSocketSession`

**Non-Goals:**
- Changing the internal implementation strategy of `BlockingNetworkRequestData` or `NIONetworkRequestData` (they still use JDK sockets internally — that's their job)
- Removing `ServerSocket`/`ServerSocketChannel` from bootstrap code (bootstraps are the edge that constructs implementations; they must bind to network ports)
- Abstracting the `Selector`/`SelectionKey` event loop in `NIOServerBootstrap` (that's the NIO event loop's concern, not the data abstraction's)
- Changing the `OutputWrapper` class (it's already a thin wrapper over `OutputStream`; the construction path will change but the class stays)
- Altering WebSocket frame wire format or protocol behavior

## Decisions

### D1: Add `close()`, `isClosed()`, `getRemoteAddress()` to `NetworkRequestData`

**Rationale**: Currently, the only way to check if a connection is alive or to close it is to grab the raw `Socket`/`SocketChannel` — which requires knowing which transport type is in use. Adding these to the interface makes lifecycle management transport-agnostic.

**Alternatives considered**:
- *Add a separate `ConnectionLifecycle` interface* — adds complexity without benefit; lifecycle is inherent to the network data abstraction
- *Use `AutoCloseable`* — too generic; `close()` with an explicit `void close() throws IOException` matches the existing I/O patterns in the codebase

### D2: Replace `ByteBuffer` with `byte[]` in `WebSocketFrameHandler` method signatures

**Rationale**: `ByteBuffer` is a mutable NIO buffer type with position/limit/capacity semantics. Handler code shouldn't need to manage buffer flipping, rewinding, or remaining checks. A plain `byte[]` is immutable in length, trivially testable, and doesn't couple handlers to `java.nio`. The `NetworkRequestData` implementation (or frame parser) is responsible for extracting bytes from whatever buffer type it uses internally.

**Alternatives considered**:
- *Use `InputStream`* — stateful, requires try/catch for reads, overkill for in-memory payloads
- *Create a custom `BytePayload` wrapper* — adds indirection without value over `byte[]` for the handler use case; `byte[]` is standard Java
- *Keep `ByteBuffer`* — leaks NIO concerns upward; handlers like `HeartBeatEndpoint` already do nothing with the buffer content

### D3: Collapse `InputWrapper` into `NetworkRequestData` implementations

**Rationale**: `InputWrapper` holds `Socket`, `SocketChannel`, `ByteBuffer`, and `InputStream` as parallel fields, with a `getInputStream()` method that picks whichever source is available. This is exactly the kind of transport multiplexing that `NetworkRequestData` implementations should own. `BlockingNetworkRequestData` already wraps `Socket.getInputStream()`; `NIONetworkRequestData` already wraps `ByteBuffer`. `InputWrapper` is a redundant abstraction sitting between them and the consumer.

**Alternatives considered**:
- *Keep `InputWrapper` but hide its fields* — it would become a pass-through to `NetworkRequestData`, making it redundant
- *Merge `InputWrapper` and `OutputWrapper` into `NetworkRequestData`* — `OutputWrapper` has a simpler role (just wrapping an `OutputStream`); keeping it separate is fine for now

### D4: `ConnectionContext` replaced by `NetworkRequestData` at the edge

**Rationale**: `ConnectionContext` is a data bag holding `Socket`, `SocketChannel`, `ByteBuffer` plus application-level metadata (`config`, `id`). After this refactor, it will hold `NetworkRequestData` plus the app-level metadata (`config`, `id`). The raw transport fields are removed. `ReadableContext` follows the same pattern.

**Alternatives considered**:
- *Remove `ConnectionContext` entirely* — it still serves a purpose as a context object carrying config; removing it would mean threading config separately through the bootstrap → handler chain
- *Keep the fields but make them private* — still leaks the types in the import/constructor surface

### D5: Bootstrap code constructs `NetworkRequestData` implementations

**Rationale**: The bootstraps (`LegacyBootstrap`, `NIOServerBootstrap`) are the only places that know which transport mode (blocking vs NIO) is in use. They already create the raw sockets/channels. After the refactor, they will immediately wrap those in the appropriate `NetworkRequestData` implementation and pass only the abstraction downstream.

## Risks / Trade-offs

- **[Risk] `byte[]` may cause extra allocation for large binary frames** → Mitigation: Frame payloads are already copied out of the `ByteBuffer` during parsing; passing the extracted `byte[]` doesn't create an additional copy. For very large frames, the existing streaming path (not yet implemented) would bypass this anyway.
- **[Risk] Adding methods to `NetworkRequestData` breaks other implementations** → Mitigation: The interface is only implemented by 2 classes in this project (`BlockingNetworkRequestData`, `NIONetworkRequestData`). No external implementations exist.
- **[Risk] `getRemoteAddress()` return type** → Mitigation: Keep returning `Object` (as `WebSocketSession` already does) since blocking returns `SocketAddress` while NIO returns `SocketAddress` — both are `Object`-compatible. A future enhancement could standardize on `InetSocketAddress`.

## Migration Plan

1. Expand `NetworkRequestData` interface with new methods first (backward-compatible addition)
2. Implement new methods in `BlockingNetworkRequestData` and `NIONetworkRequestData`
3. Add `byte[] parse(byte[])` overload to `HttpParser` (keeps existing `InputStream` overload temporarily)
4. Refactor `WebSocketSession` to remove `socket`/`socketChannel` fields; delegate to `NetworkRequestData`
5. Refactor `DefaultLegacySessionBasedConnectionHandler` to use `NetworkRequestData` methods
6. Refactor `LegacyConnectionAdapter` to not pass raw `Socket`
7. Refactor `ConnectionContext` and `ReadableContext` to use `NetworkRequestData`
8. Refactor `NIOModernIncomingSocketHandler` to eliminate raw channel operations
9. Collapse `InputWrapper` into `NetworkRequestData` implementations
10. Change `WebSocketFrameHandler` signatures from `ByteBuffer` to `byte[]`; update all handlers
11. Update `WebSocketFrameDispatcher` and `ByteBufferFrameParserImpl`
12. Remove dead code and unused imports
13. Verify compilation and run tests

Each step compiles independently — no mega-PR required.

## Open Questions

- Should `NetworkRequestData.getRemoteAddress()` return `Object` (as today) or a more specific type like `String` (the string representation)? Leaning toward `Object` for now to match existing usage in `WebSocketConnection.getRemoteAddress()`.
- Should we rename `NetworkRequestData` to something broader like `NetworkConnection` now that it handles both read/write/lifecycle? Out of scope for this refactor — naming can be addressed separately.