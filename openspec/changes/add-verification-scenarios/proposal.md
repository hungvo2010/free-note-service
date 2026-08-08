## Why

The recent refactoring to remove `MessageParser` from `AbstractEndpointHandler` delegates all string parsing to application modules. To ensure this architectural change is robust and doesn't introduce regressions, we need formal Acceptance Criteria (AC) and corresponding verification tests to prove that heartbeats, malformed inputs, core handlers, and binary messages are handled correctly.

## What Changes

- **Add Verification Specifications**: Create a new spec file detailing the required behavior for application-level heartbeat validation, malformed message handling, core handler echo behavior, and binary message passthrough.
- **Implement Unit Tests**: Add unit tests (e.g., to `FreeNoteEndpointTest`) to verify the new application-level parsing logic against the defined specifications.

## Capabilities

### New Capabilities
- `verification-tests`: Specifies and implements acceptance tests to verify the robustness of the raw message delegation architecture.

### Modified Capabilities
<!-- No existing specs to modify -->

## Impact

- **Test Suites**: New and updated tests in `free-draw/src/test/java/com/freedraw/endpoint/FreeNoteEndpointTest.java` and potentially core test suites to verify `NewEchoEndpointHandler` and binary message handling.
- **Robustness**: Provides confidence in the decoupled message handling architecture.
