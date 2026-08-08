## Why

The `NetworkRequestData` interface already exists as the core abstraction for network I/O, but low-level JDK network classes (`Socket`, `SocketChannel`, `ByteBuffer`, `InputStream`/`OutputStream`) leak through 19+ files — from domain objects like `ConnectionContext` and `WebSocketSession` all the way up to the `WebSocketFrameHandler` interface. This couples every layer to concrete transport types, prevents swapping blocking/NIO implementations transparently, and makes unit testing impossible without real sockets. We need to finish the abstraction we started: make `NetworkRequestData` the sole surface for network I/O across the entire server stack.

## What Changes

- **Expand `NetworkRequestData` interface** with lifecycle methods: `close()`, `isClosed()`, and `getRemoteAddress()` — so callers never need the underlying transport reference
- **Remove raw `Socket` and `SocketChannel` fields** from `WebSocketSession` — all I/O goes through `NetworkRequestData`; `getRemoteAddress()` delegates to it
- **Remove raw `Socket`, `SocketChannel`, and `ByteBuffer` fields** from `ConnectionContext` — replaced by `NetworkRequestData` constructed at the bootstrap layer
- **Remove raw `SocketChannel`, `ByteBuffer`, and `SelectionKey` fields** from `ReadableContext` — replaced by `NetworkRequestData`
- **Remove raw `Socket`, `SocketChannel`, `ByteBuffer`, `InputStream` fields** from `InputWrapper` — collapsed into `NetworkRequestData` implementations
- **Replace `ByteBuffer` with `byte[]`** in `WebSocketFrameHandler` and `AbstractEndpointHandler` method signatures — handlers shouldn't know about NIO buffer types
- **Make `BlockingNetworkRequestData` and `NIONetworkRequestData` self-contained** — they accept high-level config at construction, not raw `Socket`/`SocketChannel`/`ByteBuffer`
- **Refactor `DefaultLegacySessionBasedConnectionHandler`** to use `NetworkRequestData` methods instead of `session.getSocket()`
- **Refactor `NIOModernIncomingSocketHandler`** to eliminate `channel.socket().getOutputStream()` and direct `SocketChannel` operations
- **Refactor `LegacyConnectionAdapter`** to build sessions without passing raw `Socket`
- **Refactor bootstraps** (`LegacyBootstrap`, `NIOServerBootstrap`) to construct `NetworkRequestData` implementations at the edge
- **Remove `SocketChannel` getter** from `WebSocketConnection` — callers use `NetworkRequestData` instead
- **BREAKING**: `WebSocketFrameHandler` interface changes — `onMessage`, `onPing`, `onPong`, `onContinue` take `byte[]` instead of `ByteBuffer`
- **BREAKING**: `HttpParser.parse(ByteBuffer)` signature changes to `parse(byte[])`

## Capabilities

### New Capabilities

- `network-request-data-lifecycle`: `NetworkRequestData` gains `close()`, `isClosed()`, and `getRemoteAddress()` — lifecycle and metadata management without exposing the underlying transport
- `web-socket-session-abstraction`: `WebSocketSession` no longer leaks raw `Socket`/`SocketChannel`; all I/O and metadata queries go through `NetworkRequestData`
- `connection-context-abstraction`: `ConnectionContext` and `ReadableContext` hide their low-level transport fields behind `NetworkRequestData`
- `frame-handler-abstraction`: `WebSocketFrameHandler` and all endpoint handlers use `byte[]` instead of `java.nio.ByteBuffer` for message payloads

### Modified Capabilities

<!-- No existing specs to modify -->

## Impact

- **Affected files**: ~19 files across `core/connection/`, `core/context/`, `core/legacy/`, `core/nio/`, `core/startup/`, `model/`, `model/ws/`, `parser/`, `routes/`, `frames/handler/`, `routes/frames/`
- **Interface changes**: `NetworkRequestData` (3 new methods), `WebSocketFrameHandler` (4 signatures change), `HttpParser` (1 signature change)
- **Deleted fields**: `Socket` and `SocketChannel` from `WebSocketSession`; `Socket`, `SocketChannel`, `ByteBuffer` from `ConnectionContext`; `SocketChannel`, `ByteBuffer`, `SelectionKey` from `ReadableContext`; `Socket`, `SocketChannel`, `ByteBuffer`, `InputStream` from `InputWrapper`
- **No new dependencies** — uses only existing JDK and project types
