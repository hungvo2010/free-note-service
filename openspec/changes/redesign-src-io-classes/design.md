## Context

The current `InputWrapper` and `OutputWrapper` in `com.freenote.app.server.model` are essentially "bag of fields" classes. `InputWrapper` in particular tries to hold references to `Socket`, `ByteBuffer`, `SocketChannel`, and `InputStream` all at once, leading to complex null-checking logic and tight coupling with transport-specific APIs in downstream components like parsers and handlers.

## Goals / Non-Goals

**Goals:**
- Decouple the data processing logic from the transport implementation.
- Provide a unified interface for reading and writing data.
- Improve testability by allowing easy mocking of I/O sources/targets.
- Maintain support for both blocking I/O (BIO) and non-blocking I/O (NIO).

**Non-Goals:**
- Implementing a completely new transport layer (this redesign focuses on the *abstractions* used by existing layers).
- Changing the wire protocol.

## Decisions

### 1. Introduce I/O Interfaces
Instead of concrete classes with multiple optional fields, we will introduce interfaces (or use existing ones more effectively) to represent I/O operations.

- **`DataSource`**: An interface with methods like `getInputStream()`, `read(ByteBuffer dest)`, and `close()`.
- **`DataTarget`**: An interface with methods like `getOutputStream()`, `write(ByteBuffer src)`, and `close()`.

### 2. Refactor `InputWrapper` and `OutputWrapper`
Refactor these classes to wrap a `DataSource` and `DataTarget` respectively.
- `InputWrapper` will primarily delegate to its `DataSource`.
- `OutputWrapper` will delegate to its `DataTarget`.

### 3. Concrete Implementations
Provide specific implementations for common scenarios:
- `SocketDataSource` / `SocketDataTarget`
- `ChannelDataSource` / `ChannelDataTarget` (NIO)
- `InputStreamDataSource` / `OutputStreamDataTarget`

## Risks / Trade-offs

- **[Risk] Complexity in Transition** → **Mitigation**: Introduce the new interfaces alongside the old ones if necessary for a gradual migration, or use a "Big Bang" approach if the scope is manageable and well-tested.
- **[Trade-off] Performance Overhead** → **Mitigation**: Ensure that the abstraction layer is thin and doesn't introduce significant latency, especially for NIO paths where `ByteBuffer` reuse is critical.
