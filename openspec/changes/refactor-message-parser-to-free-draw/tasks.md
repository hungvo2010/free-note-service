## 1. Core Cleanup

- [x] 1.1 Remove `MessageParser` instantiation from `AbstractEndpointHandler`
- [x] 1.2 Update `AbstractEndpointHandler.onMessage(WebSocketConnection, String)` to delegate to `onData(WebSocketConnection, String)` directly (or a similar protected method)
- [x] 1.3 Remove `MessageParser.java` from the core module entirely
- [x] 1.4 Move or remove `IncomingMessage`, `DataIncomingMessage`, and `HeartbeatIncomingMessage` from the core module

## 2. Application Implementation (free-draw)

- [ ] 2.1 Update `FreeNoteEndpoint` to override the appropriate method (e.g., `onData` or `onMessage`) to receive the raw string
- [ ] 2.2 Implement JSON parsing within `FreeNoteEndpoint` to detect `HeartbeatMsg` (PING) vs standard data requests
- [ ] 2.3 Ensure `FreeNoteEndpoint` correctly handles heartbeats without breaking draft logic

## 3. Subclass Verification

- [ ] 3.1 Verify `HeartBeatEndpointHandler` handles raw messages correctly
- [ ] 3.2 Verify `NewEchoEndpointHandler` handles raw messages correctly
- [ ] 3.3 Verify `NIOCommonEndpoint` handles raw messages correctly
