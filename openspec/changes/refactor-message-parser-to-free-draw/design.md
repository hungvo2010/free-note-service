## Context

`AbstractEndpointHandler` currently forces incoming WebSocket string messages through a `MessageParser` which attempts to identify core vs. business messages (e.g., `HeartbeatMsg` vs `DataIncomingMessage`). This forces the core transport layer to know about application-level schemas, violating clean architecture.

## Goals / Non-Goals

**Goals:**
- Remove the `MessageParser` entirely from `AbstractEndpointHandler`.
- Make `AbstractEndpointHandler.onMessage(WebSocketConnection, String)` simply delegate to an abstract or protected method intended for subclasses.
- Move heartbeat and data parsing logic completely into `FreeNoteEndpoint` (and any other relevant subclasses).

**Non-Goals:**
- We are not changing binary message handling.
- We are not changing how the client sends messages, only how the server routes them internally.

## Decisions

### 1. Remove `MessageParser` from Core
We will delete the `messageParser` field from `AbstractEndpointHandler`. The core module will no longer attempt to categorize messages.

### 2. Update `AbstractEndpointHandler.onMessage`
The `onMessage(WebSocketConnection webSocketConnection, String message)` method will be updated to remove the try-catch block and parser logic. Instead, it will call an overridable method, such as `handleIncomingString(WebSocketConnection connection, String message)` or simply leave `onMessage` to be overridden by subclasses (since `WebSocketFrameHandler` already defines it, we can make it abstract or provide a default no-op implementation).

Given the current interface:
```java
    public void onMessage(WebSocketConnection webSocketConnection, String message) {
        onTextMessage(webSocketConnection, message);
    }
    protected abstract void onTextMessage(WebSocketConnection webSocketConnection, String message);
```
*(Note: We will check the exact structure during implementation, but the goal is direct delegation).*

### 3. Move Logic to `FreeNoteEndpoint`
`FreeNoteEndpoint` will override the string handling method. It will:
1. Parse the incoming JSON.
2. Check if it's a heartbeat (PING). If so, handle it.
3. Otherwise, proceed with the existing draft logic.

### 4. Cleanup Core Classes
Classes like `IncomingMessage`, `DataIncomingMessage`, and `HeartbeatIncomingMessage` can be removed or moved to the `free-draw` module if they are no longer needed by the core.

## Risks / Trade-offs

- **[Risk]** Breaking other core endpoints (`HeartBeatEndpointHandler`, `NewEchoEndpointHandler`).
- **[Mitigation]** We will update these endpoints to handle the raw string correctly (e.g., ignoring it or echoing it) since they no longer rely on the base class calling `onData`.
