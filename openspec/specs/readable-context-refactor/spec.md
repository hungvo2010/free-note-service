## Purpose

Define how ReadableContext carries NetworkRequestData instead of raw NIO types, eliminating SocketChannel, SelectionKey, and ByteBuffer leaks into handler code.

## Requirements

### Requirement: ReadableContext carries NetworkRequestData instead of raw NIO types
`ReadableContext` SHALL carry a `NetworkRequestData` instance instead of raw `SocketChannel`, `SelectionKey`, and `ByteBuffer` fields. The raw transport fields MUST be removed.

#### Scenario: Construct ReadableContext with NetworkRequestData
- **WHEN** `ReadableContext.builder().networkRequestData(nioNetworkRequestData).tracingContext(tracingContext).build()` is called
- **THEN** a valid `ReadableContext` is created without any `SocketChannel`, `SelectionKey`, or `ByteBuffer` fields

#### Scenario: Close connection via ReadableContext
- **WHEN** `readableContext.closeChannel()` is called
- **THEN** it delegates to `networkRequestData.close()` instead of calling `channel.close()` directly

#### Scenario: Get remote address via ReadableContext
- **WHEN** `readableContext.getRemoteAddress()` is called
- **THEN** it delegates to `networkRequestData.getRemoteAddress()` instead of calling `channel.getRemoteAddress()`

### Requirement: ReadableContext must not expose SelectionKey
`ReadableContext` SHALL NOT expose `SelectionKey` to consumers. State management (`setState()`) SHALL NOT call `key.attach()` — state SHALL be managed externally by the selector loop or through a separate mechanism.

#### Scenario: State is managed externally
- **WHEN** the selector loop needs to attach processing state to a connection
- **THEN** it manages state through its own internal mapping (e.g., a `Map<NetworkRequestData, ProcessingState>`) rather than `key.attach()`

### Requirement: ReadableContext read operation via NetworkRequestData
The `emptyReadFromChannel` check SHALL be performed via `NetworkRequestData.read()` rather than direct `channel.read()`.

#### Scenario: Check for empty read
- **WHEN** checking whether a channel read returned EOF
- **THEN** `networkRequestData.read(byteBuffer)` is called, and if it returns -1, `networkRequestData.close()` is invoked
