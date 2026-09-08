## Purpose

Define how `ConnectionContext` wraps `NetworkRequestData` instead of raw transport types (Socket, SocketChannel, ByteBuffer).

## Requirements

### Requirement: ConnectionContext carries NetworkRequestData
`ConnectionContext` SHALL carry a `NetworkRequestData` instance instead of raw `Socket`, `SocketChannel`, and `ByteBuffer` fields. The raw transport fields MUST be removed.

#### Scenario: Construct ConnectionContext with NetworkRequestData
- **WHEN** `ConnectionContext.builder().networkRequestData(networkRequestData).config(config).build()` is called
- **THEN** a valid `ConnectionContext` is created with the network abstraction embedded

#### Scenario: Access network operations via NetworkRequestData
- **WHEN** a consumer calls `context.getNetworkRequestData()`
- **THEN** it receives the `NetworkRequestData` instance that can perform all read, write, close, and lifecycle operations
