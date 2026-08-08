## ADDED Requirements

### Requirement: Annotation definition
The `@WebSocketEndpoint` annotation SHALL be a runtime-retained, type-level annotation with a single `value()` attribute specifying the WebSocket URI path.

#### Scenario: Annotation applied to a handler class
- **WHEN** a class is annotated with `@WebSocketEndpoint("/echo")`
- **THEN** the annotation is available at runtime via reflection, and the `value()` returns `"/echo"`

#### Scenario: Annotation with default value
- **WHEN** a class is annotated with `@WebSocketEndpoint` without an explicit path
- **THEN** the `value()` returns `""` (empty string)

### Requirement: WebServerProcessor validates handler signatures
The `WebServerProcessor` annotation processor SHALL validate at compile time that every `@WebSocketEndpoint`-annotated class has exactly one public method with exactly two parameters of type `InputStream` and `OutputStream`.

#### Scenario: Valid handler class
- **WHEN** a class annotated with `@WebSocketEndpoint("/echo")` has a single public method `handle(InputStream, OutputStream)`
- **THEN** compilation succeeds with no errors

#### Scenario: Missing handle method
- **WHEN** a class annotated with `@WebSocketEndpoint("/echo")` has no public method named `handle`
- **THEN** compilation fails with an error message indicating the method signature requirement

#### Scenario: Wrong parameter types
- **WHEN** a class annotated with `@WebSocketEndpoint("/echo")` has a `handle` method with parameters other than `(InputStream, OutputStream)`
- **THEN** compilation fails with an error message

#### Scenario: Empty path value
- **WHEN** a class is annotated with `@WebSocketEndpoint("")`
- **THEN** compilation fails with an error indicating the path cannot be empty

#### Scenario: Annotation applied to non-class element
- **WHEN** `@WebSocketEndpoint` is applied to a non-class element (e.g., a method or field)
- **THEN** compilation fails with an error indicating the annotation can only be applied to classes

### Requirement: BeanProviderProcessor generates URIHandlerRegistry
The `BeanProviderProcessor` annotation processor SHALL scan all `@WebSocketEndpoint`-annotated classes within the current compilation module and generate a `generated.URIHandlerRegistry` class that maps URI paths to handler instances.

#### Scenario: Single annotated class in module
- **WHEN** a compilation module contains one class annotated with `@WebSocketEndpoint("/echo")`
- **THEN** `generated.URIHandlerRegistry` is created with a static initializer that instantiates the handler and maps `"/echo"` to it

#### Scenario: Multiple annotated classes in module
- **WHEN** a compilation module contains classes annotated with `@WebSocketEndpoint("/echo")` and `@WebSocketEndpoint("/heartbeat")`
- **THEN** the generated `URIHandlerRegistry` maps both `"/echo"` and `"/heartbeat"` to their respective handler instances

#### Scenario: No annotated classes in module
- **WHEN** a compilation module contains no `@WebSocketEndpoint`-annotated classes
- **THEN** no `URIHandlerRegistry` is generated for that module

### Requirement: Generated URIHandlerRegistry API
The generated `URIHandlerRegistry` class SHALL provide static methods to look up handler instances by URI path, by class name, and by class type.

#### Scenario: Lookup by URI path
- **WHEN** `URIHandlerRegistry.getInstanceByURI("/echo")` is called and `"/echo"` is registered
- **THEN** it returns the handler instance associated with `"/echo"`

#### Scenario: Lookup by unregistered URI
- **WHEN** `URIHandlerRegistry.getInstanceByURI("/nonexistent")` is called
- **THEN** it returns `null`

#### Scenario: Lookup by class name
- **WHEN** `URIHandlerRegistry.getInstance("NewEchoEndpoint")` is called and that class is registered
- **THEN** it returns the `NewEchoEndpoint` instance

#### Scenario: Typed getter method
- **WHEN** `URIHandlerRegistry.getNewEchoEndpoint()` is called
- **THEN** it returns the `NewEchoEndpoint` instance cast to the correct type

### Requirement: Module-scoped registry generation
The annotation processor SHALL generate a separate `URIHandlerRegistry` per compilation module. Each registry SHALL only include handlers from classes within that module.

#### Scenario: Multi-module project with overlapping URIs
- **WHEN** module A has `@WebSocketEndpoint("/echo")` on class `BlockingEchoEndpoint` and module B has `@WebSocketEndpoint("/echo")` on class `NIOEchoEndpoint`
- **THEN** each module's `URIHandlerRegistry` maps `"/echo"` to its own implementation, and the resolution depends on which module's registry is on the classpath

#### Scenario: Separate handler sets per module
- **WHEN** module A has endpoints `/echo` and `/heartbeat` and module B has endpoints `/echo` and `/freeNote`
- **THEN** module A's launcher can route to `/heartbeat` but not `/freeNote`, and module B's launcher can route to `/freeNote` but not `/heartbeat`

### Requirement: Route resolution in DefaultLegacySessionBasedConnectionHandler
`DefaultLegacySessionBasedConnectionHandler` SHALL resolve the WebSocket handler for a given HTTP upgrade request by calling `getInstanceByURI(upgradeRequest.getPath())` on the module's generated `URIHandlerRegistry`.

#### Scenario: Request to registered URI
- **WHEN** an HTTP upgrade request targets a path registered in the module's `URIHandlerRegistry`
- **THEN** the corresponding handler's `handle()` method is invoked for the connection

#### Scenario: Request to unregistered URI
- **WHEN** an HTTP upgrade request targets a path not registered in the module's `URIHandlerRegistry`
- **THEN** an `AcceptConnectionException` is thrown with a message indicating no handler for that URI
