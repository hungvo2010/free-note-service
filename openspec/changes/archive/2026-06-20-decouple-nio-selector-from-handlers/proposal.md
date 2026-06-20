## Why

`NIOIncomingSocketHandler` mixes Selector-level I/O (`channel.read()`, `ByteBuffer.wrap()`, `channel.write()`) with business-level routing and handshake logic in a single 136-line class. `ReadableContext` carries raw NIO primitives (`SocketChannel`, `SelectionKey`, `ByteBuffer`) alongside business context — exposing the selector state machine to handler code via `key.attach()`. This violates SRP (one class changes for both I/O optimization and business rule changes), mixes abstraction levels (infrastructure vs policy), and makes handlers untestable without standing up a real NIO selector. The blocking path was already cleaned up in the prior `NetworkRequestData` refactor; the NIO path was explicitly deferred and is now the next priority.

## What Changes

- **Extract NIO I/O operations into a dedicated selector loop class** — `channel.read()`, `channel.write()`, `ByteBuffer` lifecycle, and `SelectionKey` management move out of the handler
- **Refactor `ReadableContext`** — remove raw `SocketChannel`, `SelectionKey`, `ByteBuffer` fields; carry `NetworkRequestData` instead (this was deferred from the prior refactor)
- **Introduce a Dispatcher** — translates raw I/O readiness events from the selector into typed business events (handshake complete, message received), decoupling "when data is ready" from "what to do with it"
- **Remove `channel.socket().getOutputStream()`** — replace with `networkRequestData.write(byte[])` (already available on `NIONetworkRequestData`)
- **Remove `ModernIncomingConnectionHandler` interface** — if it only serves the NIO path and can be unified with `IncomingConnectionHandler`
- **No changes to endpoint handlers** — `FragmentedEndpoint`, `NewEchoEndpoint`, etc. already consume `NetworkRequestData` + `OutputWrapper`

## Capabilities

### New Capabilities

- `nio-selector-abstraction`: Separate the NIO Selector event loop from handler dispatch — a dedicated IO thread runs `select()` and a Dispatcher translates readiness events to handler invocations
- `readable-context-refactor`: `ReadableContext` carries `NetworkRequestData` instead of raw `SocketChannel`, `SelectionKey`, and `ByteBuffer`

### Modified Capabilities

- `network-request-data-lifecycle`: `NIONetworkRequestData` already implements `close()`, `isClosed()`, `getRemoteAddress()` — `ReadableContext` and `NIOIncomingSocketHandler` must use these instead of raw channel operations
- `web-socket-session-abstraction`: The "callers must not depend on Socket or SocketChannel" requirement extends to NIO path handlers

## Impact

- Affected code: `NIOIncomingSocketHandler`, `ReadableContext`, `NIOWebSocketServer`, `NIOServerBootstrap`, `NIONetworkRequestData`, `ModernIncomingConnectionHandler`
- No API changes to endpoint handlers (`URIEndpointHandler` consumers unaffected)
- **BREAKING**: `ReadableContext` field types change (raw NIO → `NetworkRequestData`); `NIOIncomingSocketHandler` public API changes
- Related: `ProcessingState` / `ConnectionState` state machine classes (NIO-specific, may be simplified)
