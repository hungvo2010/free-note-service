## Why

`AbstractEndpointHandler` in the core module currently has a hard-coded dependency on `MessageParser`, which parses raw messages into business-level `IncomingMessage` objects (like `HeartbeatMsg` and `DataIncomingMessage`). This violates clean architecture principles because message parsing is business logic, and the transport layer (core) should not dictate or depend on application-specific message formats. By completely removing the parser from the core and simply delegating raw strings, we make the core truly generic and decoupled.

## What Changes

- **Core Refactoring (**BREAKING**)**: Remove the `MessageParser` instantiation and usage from `AbstractEndpointHandler`. The core handler will no longer try to parse strings into `IncomingMessage` objects.
- **Method Signature Update**: Change the `onMessage(WebSocketConnection, String)` method in `AbstractEndpointHandler` to simply delegate the raw string to a new abstract method (or let subclasses override it directly).
- **Application Logic Migration**: Move all parsing logic (including heartbeat detection) directly into the application-level handlers (e.g., `FreeNoteEndpoint` in the `free-draw` module).

## Capabilities

### New Capabilities
- `raw-message-delegation`: The core system delegates raw string message handling entirely to application modules without imposing a parsing structure.

### Modified Capabilities
<!-- No existing specs to modify -->

## Impact

- **Core Module**: `AbstractEndpointHandler` and `com.freenote.app.server.parser.MessageParser` (parser class will be moved/removed). `IncomingMessage` interface and related classes will be moved out of core if they are only used for parsing.
- **Free-Draw Module**: `FreeNoteEndpoint` will take over full responsibility for parsing raw incoming strings, detecting heartbeats, and handling business logic.
- **Inheritance**: All subclasses of `AbstractEndpointHandler` that relied on the base class calling `onData` or handling heartbeats automatically will need to be updated to handle the raw `onMessage` directly.
