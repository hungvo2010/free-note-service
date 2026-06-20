## ADDED Requirements

### Requirement: ConnectionContext carries NetworkRequestData
`ConnectionContext` SHALL carry a `NetworkRequestData` instance instead of raw `Socket`, `SocketChannel`, and `ByteBuffer` fields. The raw transport fields MUST be removed.

#### Scenario: Construct ConnectionContext with NetworkRequestData
- **WHEN** `ConnectionContext.builder().networkRequestData(networkRequestData).config(config).build()` is called
- **THEN** a valid `ConnectionContext` is created with the network abstraction embedded

#### Scenario: Access network operations via NetworkRequestData
- **WHEN** a consumer calls `context.getNetworkRequestData()`
- **THEN** it receives the `NetworkRequestData` instance that can perform all read, write, close, and lifecycle operations

### Requirement: ReadableContext carries NetworkRequestData

> **DEFERRED to NIO-specific refactor.** `ReadableContext` is part of the NIO selector event loop, where `SocketChannel`+`ByteBuffer`+`SelectionKey` are tightly coupled to the selector state-machine pattern (`key.attach(state)`, buffer reuse across events). The blocking path does not use `ReadableContext`. This requirement will be addressed in a separate NIO-specific change.

### Requirement: LegacyConnectionAdapter uses NetworkRequestData, not raw Socket
`LegacyConnectionAdapter` SHALL construct a `BlockingNetworkRequestData` from the `ConnectionContext.getNetworkRequestData()` or from available transport info at the edge, and SHALL NOT pass a raw `Socket` to the `WebSocketSession` builder.

#### Scenario: Adapter converts context to session
- **WHEN** `LegacyConnectionAdapter.handle(context)` is called
- **THEN** it creates a `WebSocketSession` with only `networkRequestData` set (no `.socket()` builder call)