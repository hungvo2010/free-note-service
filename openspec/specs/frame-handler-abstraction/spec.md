## Purpose

Define how HTTP parsing and frame handling work with `NetworkRequestData` to avoid raw InputStream/Socket dependencies in handler code.

## Requirements

### Requirement: HttpParser provides byte[] overload
The `HttpParser` interface SHALL provide an `HttpUpgradeRequest parse(byte[] data) throws IOException` overload, so callers can pass bytes read via `NetworkRequestData.read()` without constructing an `InputStream`.

#### Scenario: Parse HTTP upgrade from bytes
- **WHEN** `httpParser.parse(byteArray)` is called with raw HTTP upgrade request bytes
- **THEN** it returns an `HttpUpgradeRequest` parsed from those bytes
