## Context

The `@WebSocketEndpoint` annotation and its processors were added early in the project's life without documentation. Two annotation processors now operate on the same annotation:

- **`WebServerProcessor`** — compile-time validator. Checks that annotated classes expose exactly one `handle(InputStream, OutputStream)` method. This reflects the original blocking-I/O handler contract (`URIEndpointHandler`). The validator predates the `URIEndpointHandler` interface itself.
- **`BeanProviderProcessor`** — code generator. Produces `generated.URIHandlerRegistry` with URI→handler instance mappings, typed getters, and the `getInstanceByURI(String)` lookup used at runtime.

Both processors are registered via Google AutoService and run in the same annotation processing round per compilation module. The module boundary is critical: each Gradle subproject or source set that contains `@WebSocketEndpoint`-annotated classes gets its own `URIHandlerRegistry`.

## Goals / Non-Goals

**Goals:**
- Document the annotation definition, its two processors, and the generated registry contract
- Explain the module-scoped generation model and its implications for multi-module projects
- Trace the full route resolution path from annotation → processor → generated code → runtime dispatch

**Non-Goals:**
- Change the annotation or processor behavior
- Unify the two processors (though noted as a future improvement)
- Change the handler interface contract validated by `WebServerProcessor`

## Decisions

### Why two separate processors?

`WebServerProcessor` and `BeanProviderProcessor` were implemented independently. `WebServerProcessor` enforces the old `handle(InputStream, OutputStream)` contract; `BeanProviderProcessor` generates the registry. They could be merged into a single processor — the validator could run before generation in the same `process()` call — but this is deferred.

### Why module-scoped generation?

The annotation processor uses `processingEnv.getFiler()` which writes to the current compilation unit's output directory. In a multi-module Gradle build, each module gets its own generated sources directory. This is standard Java annotation processing behavior, not a deliberate design choice — but it creates the side effect that different launchers in different modules see different handler sets.

### Why `value()` for the URI path?

The annotation's `value()` attribute follows Java convention for single-attribute annotations. The empty-string default forces the developer to supply a path or be rejected by `WebServerProcessor`.

## Risks / Trade-offs

- **[URI collision across modules]**: Both the main module and free-draw register `/echo`, but with different implementations (`NewEchoEndpoint` vs `NIOEchoEndpoint`). If both generated registries end up on the same classpath, `getInstanceByURI("/echo")` behavior is undefined (depends on classloader order). → Mitigation: Modules are kept on separate classpaths via Gradle subproject boundaries. Formalizing this constraint in build config is a future improvement.
- **[Stale validation contract]**: `WebServerProcessor` validates `handle(InputStream, OutputStream)` but `URIEndpointHandler` now takes `NetworkRequestData` and `OutputWrapper`. The validator is out of sync with the actual interface. → Noted as tech debt; processor should validate against `URIEndpointHandler` instead of raw parameter types.
- **[Processor ordering]**: Both processors run in unspecified order within the same round. Since `BeanProviderProcessor` only generates code (doesn't validate), and `WebServerProcessor` only validates (doesn't generate), order doesn't matter — but if merged or extended, ordering constraints should be documented.

## Open Questions

- Should `WebServerProcessor` and `BeanProviderProcessor` be merged into a single processor?
- Should the generated registry include a module identifier to make URI conflicts explicit at startup?
- Should `WebServerProcessor` validate against the `URIEndpointHandler` interface instead of raw parameter types?
