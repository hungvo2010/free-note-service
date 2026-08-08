## ADDED Requirements

### Requirement: Application Heartbeat Validation
The application layer SHALL handle heartbeat messages to maintain connection health.

#### Scenario: Successful PONG response
- **WHEN** the `FreeNoteEndpoint` receives a JSON message with `msgType` set to `PING`
- **THEN** it SHALL return a JSON message with `msgType` set to `PONG`.

### Requirement: Resilience to Malformed Input
The system SHALL handle invalid string messages without terminating the connection.

#### Scenario: Malformed JSON handling
- **WHEN** a subclass of `AbstractEndpointHandler` receives a message that is not valid JSON
- **THEN** it SHALL log the error and either ignore the message or send a default response, ensuring the handler remains active.

### Requirement: Regression Testing for Core Handlers
Core handlers MUST maintain their existing functionality after the parser removal.

#### Scenario: Echo handler consistency
- **WHEN** the `NewEchoEndpointHandler` receives a raw string
- **THEN** it SHALL echo the exact same string back to the client.

### Requirement: Binary Message Integrity
The refactor SHALL NOT affect the processing of binary WebSocket frames.

#### Scenario: Binary message routing
- **WHEN** a binary message is received
- **THEN** the system SHALL route it directly to `onBinaryMessage` without attempting any string-based parsing or delegation.
