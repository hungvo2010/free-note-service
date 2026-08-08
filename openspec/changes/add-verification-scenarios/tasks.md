## 1. Application-Level Tests (free-draw)

- [ ] 1.1 Create `FreeNoteEndpointTest.java`
- [ ] 1.2 Implement test to verify `FreeNoteEndpoint.onData` correctly handles a JSON string representing a `HeartbeatMsg` (PING -> PONG)
- [ ] 1.3 Implement test to verify `FreeNoteEndpoint.onData` handles invalid JSON strings without crashing the connection

## 2. Core Handler Tests (src)

- [ ] 2.1 Update or create `NewEchoEndpointHandlerTest.java`
- [ ] 2.2 Implement test to verify `NewEchoEndpointHandler` echoes back raw strings exactly as received
- [ ] 2.3 Verify `AbstractEndpointHandler` correctly delegates binary `ByteBuffer` messages to `onBinaryMessage`
