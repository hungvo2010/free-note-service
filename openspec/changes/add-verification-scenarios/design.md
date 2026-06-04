## Context

The core WebSocket handling logic has been decoupled from application-specific message formats by removing the `MessageParser` from `AbstractEndpointHandler`. This requires the application layer, such as `FreeNoteEndpoint`, to explicitly handle tasks like heartbeat processing. To ensure these changes are correct, we need a suite of unit tests.

## Goals / Non-Goals

**Goals:**
- Provide clear unit test designs for the newly delegated message parsing logic.
- Ensure the `FreeNoteEndpoint` correctly handles and responds to `HeartbeatMsg` (PING -> PONG).
- Ensure core handlers like `NewEchoEndpointHandler` continue to function as expected.
- Validate robustness against malformed input.

**Non-Goals:**
- We are not writing full integration tests or end-to-end WebSocket tests, only unit tests for the handlers.

## Decisions

### 1. Mocking Strategy
We will use `Mockito` to mock the `WebSocketConnection`. We will invoke the `onData` or `onMessage` methods directly on the handler instances, passing in raw strings, and use `ArgumentCaptor` to capture the responses set on the mocked connection.

### 2. Testing `FreeNoteEndpoint`
We will create or update `FreeNoteEndpointTest`.
- **Test 1:** Pass a JSON string representing a `HeartbeatMsg` (PING). Verify that `webSocketConnection.setResponseObject()` is called with a `CommonResponseObject` containing a `HeartbeatMsg` with type `PONG`.
- **Test 2:** Pass an invalid JSON string. Verify that the handler catches the exception and gracefully sets the default response without throwing the exception back up to the caller.

### 3. Testing Core Handlers
We will create or update tests for `NewEchoEndpointHandler` and potentially `HeartBeatEndpointHandler` to ensure they handle the raw strings properly since they no longer rely on `MessageParser`.
- **Echo Test:** Call `onMessage` with a random string and verify that the exact same string is sent back via the connection mock.

## Risks / Trade-offs

- **[Risk]** The unit tests might be tightly coupled to the internal implementation details of how the connection is mocked.
- **[Mitigation]** Focus on verifying the *outputs* (`setResponseObject`, `sendText`) rather than internal state changes.
