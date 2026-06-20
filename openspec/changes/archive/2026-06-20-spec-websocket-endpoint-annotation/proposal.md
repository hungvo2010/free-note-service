## Why

The `@WebSocketEndpoint` annotation and its annotation processors form the backbone of how WebSocket routes are discovered, validated, and resolved at compile time — yet this mechanism is entirely undocumented. Two annotation processors (`WebServerProcessor` for validation, `BeanProviderProcessor` for code generation) run per compilation module, producing module-scoped `URIHandlerRegistry` classes. The module-scoped nature means different launchers see different endpoint sets, and a `/echo` route can resolve to completely different implementations depending on which module the launcher lives in. Without specs, this behavior is undiscoverable and fragile.

## What Changes

- Document the `@WebSocketEndpoint` annotation definition: retention policy, target, and `value()` semantics
- Spec the `WebServerProcessor`: compile-time validation of annotated handler classes (method signature checks)
- Spec the `BeanProviderProcessor`: code generation of `URIHandlerRegistry` with URI→handler mapping
- Document the module-scoped registry generation and its implications for multi-module builds
- Document the route resolution path from HTTP upgrade request to handler dispatch
- **No code changes** — this is a spec-only change documenting existing behavior

## Capabilities

### New Capabilities

- `websocket-endpoint-annotation`: The `@WebSocketEndpoint` annotation definition, its annotation processors, the generated `URIHandlerRegistry`, and the route resolution pipeline from annotation to handler dispatch

### Modified Capabilities

<!-- None — this documents existing behavior, no requirement changes -->

## Impact

- Affected code: `com.freenote.annotations.WebSocketEndpoint`, `com.freenote.processors.WebServerProcessor`, `com.freenote.processors.BeanProviderProcessor`, `generated.URIHandlerRegistry`, `DefaultLegacySessionBasedConnectionHandler`
- No API changes, no dependency changes, no breaking changes
