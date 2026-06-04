## ADDED Requirements

### Requirement: Raw Message Delegation
The core `AbstractEndpointHandler` SHALL NOT parse incoming string messages. It MUST delegate raw string messages directly to its subclasses.

#### Scenario: Delegating a raw message
- **WHEN** a WebSocket string message is received by the core handler
- **THEN** it passes the exact string to an abstract or overridable method implemented by the subclass without attempting to construct an `IncomingMessage` object.

### Requirement: Application-Level Parsing
Application modules (like `free-draw`) SHALL take full responsibility for parsing the raw string messages they receive.

#### Scenario: Handling business logic in free-draw
- **WHEN** the `FreeNoteEndpoint` receives a raw string message
- **THEN** it parses the JSON, determines if it is a heartbeat or data request, and processes it accordingly.
