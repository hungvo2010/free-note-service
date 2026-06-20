## ADDED Requirements

### Requirement: HttpParser provides byte[] overload
The `HttpParser` interface SHALL provide an `HttpUpgradeRequest parse(byte[] data) throws IOException` overload, so callers can pass bytes read via `NetworkRequestData.read()` without constructing an `InputStream`. **DONE.**

#### Scenario: Parse HTTP upgrade from bytes
- **WHEN** `httpParser.parse(byteArray)` is called with raw HTTP upgrade request bytes
- **THEN** it returns an `HttpUpgradeRequest` parsed from those bytes

### Requirement: Handler interface uses byte[] for payloads

> **DEFERRED.** `WebSocketFrameHandler` serves both blocking and NIO paths. Changing `ByteBuffer`→`byte[]` would break the NIO path which still uses `ByteBuffer` internally. This requirement will be addressed after the NIO path is also migrated to `NetworkRequestData`.

### Requirement: Endpoint handlers must not import ByteBuffer for payloads

> **DEFERRED.** Depends on `WebSocketFrameHandler` interface change (above).

### Requirement: Frame dispatcher converts ByteBuffer to byte[] at the boundary

> **DEFERRED.** Depends on `WebSocketFrameHandler` interface change (above).