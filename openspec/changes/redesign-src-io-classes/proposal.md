## Current State Analysis

Before the redesign, here is how the existing classes in com.freenote.app.server.model and com.freenote.app.server.core are categorized based on their current purpose in serving incoming requests and outgoing responses:

### 1. Transport Abstraction The Bridge
These classes wrap low-level network resources to provide a semi-unified interface for data I/O.
- **InputWrapper**: Consolidates Socket, SocketChannel, and InputStream. Its main job is to provide an InputStream to consumers, regardless of whether the connection is BIO or NIO based.
- **OutputWrapper**: A thin wrapper around OutputStream for sending bytes back to the client.

### 2. Protocol Negotiation The Doorway
Handles the initial HTTP handshake required to transition to the WebSocket protocol.
- **HttpUpgradeRequest**: Captures the initial HTTP GET headers e.g., Sec-WebSocket-Key, Upgrade from the client.
- **HttpUpgradeResponse**: Represents the 101 Switching Protocols response sent back to acknowledge the upgrade.

### 3. Application Message Envelopes The Container
Generic containers that wrap the actual domain logic data after the connection is upgraded.
- **CommonRequestObject<T>**: Carries the Socket, origin, and the generic request payload.
- **CommonResponseObject<T>**: Carries the generic response payload back to the sender.

### 4. Observability & Metadata The Passport
Provides tracking information that persists across the request/response lifecycle.
- **TraceRequestData**: Generates and holds requestId and traceId when a request first enters the system.
- **TraceResponseData**: Carries the same IDs in the response to allow end-to-end correlation.

### 5. Session & Lifecycle The State
Manages the long-term state of a connected client.
- **WebSocketConnection**: Maintains the active connection state and provides methods to send data or close the connection.
- **WebSocketSession**: Holds user-specific data associated with an active connection.

### 6. Handling & Orchestration The Traffic Controller
Orchestrates the flow of data between the network and the application logic.
- **IncomingConnectionHandler**: The base interface for components that react to new data on a connection.
- **NIOModernIncomingSocketHandler**: Implements the complex logic of reading from NIO channels into buffers and delegating to parsers.

---

## Why

The current InputWrapper and OutputWrapper classes are tightly coupled with specific transport implementations Sockets, ByteBuffers, InputStreams and have overlapping responsibilities. Redesigning these will improve modularity, testability, and provide a unified interface for handling data flow regardless of the underlying transport.

## What Changes

- Redesign InputWrapper to use a more abstract and consistent approach for data retrieval.
- Enhance OutputWrapper to support more than just OutputStream, potentially including SocketChannel or ByteBuffer writes.
- Introduce clear interfaces or abstract classes to decouple transport details from business logic.
- **BREAKING**: Existing consumers of InputWrapper and OutputWrapper will need to be updated to the new API.

## Capabilities

### New Capabilities
- unified-io-abstractions: Define a common set of interfaces for reading and writing data that abstracts away whether the source is a traditional Socket, an NIO SocketChannel, or an in-memory buffer.

### Modified Capabilities
<!-- No requirement changes to existing specs. -->

## Impact

- com.freenote.app.server.model.InputWrapper
- com.freenote.app.server.model.OutputWrapper
- All classes in com.freenote.app.server.handler and com.freenote.app.server.parser that consume these wrappers.
