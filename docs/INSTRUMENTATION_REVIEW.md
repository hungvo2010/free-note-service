# Instrumentation Review

Status: analysis
Scope: the telemetry path in `src/main/java/com/freenote/app/server/`, the `observability/`
module, and `PersistenceManager` in `free-draw`
Created: 2026-09-22

## Method

Every claim below was read out of the source at the line cited. Best-practice claims are drawn
from OpenTelemetry primary sources that were fetched and verified — the specification's Context
and Trace-API chapters, the Java API reference, and the maintainers' own `Context.java` — rather
than recalled. Where OTel is silent, this document says so instead of inventing guidance.

## Findings, by severity

| # | Finding | Where | Severity |
|---|---|---|---|
| 1 | Every span in the server core is started and never ended | `ConnectionPipeline:78-86` | Critical |
| 2 | Context is hand-rolled instead of `Context` — and never read | `TracingContext`, `ReadableContext:14` | Critical |
| 3 | No context propagation across any thread boundary | 3 I/O models, 0 wrap sites | High |
| 4 | Telemetry coupled into the core; 11 static call sites, no seam | 6 classes | High |
| 5 | Span attributes silently truncated (3–4 set, limit 2) | `SpanLimitsConfig:9` | High |
| 6 | Duplicate and synchronous exporters on the message path | `SdkTracerProviderConfig:23-26` | High |
| 7 | Latency histogram off by 1000× | `OtelLatencyMetric:50-56` | High |
| 8 | Start-up ordering can silently no-op all telemetry | `SampleGlobalOpenTelemetry:30-36` | Medium |
| 9 | Propagators configured but never used | `ContextPropagatorsConfig` | Medium |

---

## 1. Every span in the server core leaks

`ConnectionPipeline.buildSpan` starts a span on **every handshake and every message**:

```java
private Span buildSpan(ConnectionState state) {
    String spanName = state instanceof HandShakeState ? "websocket.handshake" : "websocket.message";
    return getSampleGlobalTelemetry().getTracer().spanBuilder(spanName)
            .setAttribute("server.address", "localhost")
            .setAttribute("server.port", -1)
            .setAttribute("network.transport", "tcp")
            .setAttribute("app.websocket.state", state.getClass().getSimpleName())
            .startSpan();
}
```

It is called from `buildConnectionContext` (`:59`). The returned span goes into `TracingContext`
into `ReadableContext` into `ConnectionContext`, and **no `span.end()` exists anywhere on this
path**. Grepping `.end()` across `src/` and `free-draw/` returns four hits, all in
`PersistenceManager`.

The spec is unambiguous: *"Any span that is created MUST also be ended. This is the
responsibility of the user."* An unended span has no end timestamp, is never exported by a
processor that exports on `onEnd`, and accumulates in the SDK.

**The correct pattern already exists in this repo.** `PersistenceManager` ends spans in `finally`
blocks and sets status on failure (`:63-67`, `:122-127`):

```java
} catch (Exception e) {
    span.recordException(e);
    span.setStatus(StatusCode.ERROR, e.getMessage());
    throw e;
} finally {
    span.end();
}
```

So this is not a knowledge gap in the codebase — it is the server core not following the pattern
the persistence layer already uses.

**A design question the spec does not answer.** The spec defines a span as "a single operation"
and explicitly "does not describe long-running processes". A WebSocket connection living for hours
is not that. There is **no WebSocket semantic convention** — `specs/semconv/messaging/websocket/`
is a 404, and WebSocket is absent from the messaging index. The nearest guidance (messaging spans,
status *Development*) is message-oriented and silent on long-lived connections. So connection-span
vs message-span is a decision this codebase has to make and record, not look up.

## 2. Context is hand-rolled, and never read

`otel/sdk/context/TracingContext` is a mutable POJO wrapping a span:

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TracingContext {
    private Span span;
}
```

`@Data` generates setters — a mutable carrier for an object the spec treats as immutable and
execution-scoped. It is threaded `ConnectionPipeline` → `ReadableContext` → `ConnectionContext` →
`PerConnectionHandler` → `ConnectionState.transition`, and **nothing ever reads it**:
`getTracingContext()` has zero call sites, and the only references are the field declaration and
the builder call.

OTel already provides this mechanism, and the codebase does not use it. There is **no
`makeCurrent()`, no `Context.current()`, no `Span.fromContext`, and no `Scope`** anywhere in the
repo. The spec says span creation "MUST NOT set the newly created `Span` as the active `Span`" by
default — activation is a separate, explicit act that was skipped entirely.

The maintainers' warning in `Context.java` names this exact anti-pattern:

> Context is not intended for passing optional parameters to an API and developers should take
> care to avoid excessive dependence on context when designing an API.

`TracingContext` is a parameter passed down the call stack — a hand-rolled re-implementation of
`io.opentelemetry.context.Context`, minus the immutability, minus the thread-local storage, minus
the propagation, and minus any reader.

## 3. No propagation across any thread boundary

All three I/O models hop threads, and none of them carry context:

| Path | Thread hop | Context handling |
|---|---|---|
| `LegacyBootstrap:53` | `Executors.newVirtualThreadPerTaskExecutor()` per connection | none |
| `NIOServerBootstrap:53` | `Executors.newFixedThreadPool(2)` for the selector loop | none |
| `AsyncNIOServerSession` | NIO.2 `CompletionHandler` callbacks on pool threads | none |

`ContextStorage` is **thread-local by default**, and `ThreadLocal` is not inherited — so every one
of those hops is a propagation decision someone must make explicitly. There is no
`Context.wrap(...)`, no `Context.taskWrapping(...)`, and no wrapped executor anywhere in the repo.

Virtual threads need this more than platform threads, not less. `Thread.ofVirtual().start()`,
`Thread.startVirtualThread(...)` and `Thread.ofPlatform().start(...)` do **not** propagate the
parent context — an open issue on the maintainers' tracker (`opentelemetry-java-instrumentation`
#11950) — and the agent's fix (commit `628136e`) *disables* context propagation when a virtual
thread switches to its carrier thread, precisely so context cannot leak onto a shared carrier.
Given `LegacyBootstrap` runs one virtual thread per connection, this is not hypothetical here.

## 4. Telemetry is coupled into the core

Eleven static call sites, none injectable:

| Class | Call sites |
|---|---|
| `AbstractEndpointHandler` | `:40`, `:42`, `:53` |
| `ThreadPerConnectionHandler` | `:58`, `:76`, `:88` |
| `AsyncNIOServerSession` | `:54`, `:76`, `:89` |
| `NIOServerBootstrap` | `:108` |
| `NIOServerSession` | `:106` |

More significant than the count is the direction of the dependency. The server core reaches into
the observability module's *concrete singleton* rather than an interface it owns:

```java
// ConnectionPipeline.java:18
import static otel.SampleGlobalOpenTelemetry.getSampleGlobalTelemetry;
```

and `core/context/ReadableContext` imports `otel.sdk.context.TracingContext`. The core therefore
depends on `observability`, cannot be exercised without it, and cannot be substituted in a test —
`MetricUtils` has no interface and no constructor.

`PersistenceManager` — a persistence implementation — holds a `static Tracer` and a
`static DoubleHistogram` captured at class load:

```java
private static final Tracer tracer = getSampleGlobalTelemetry().getTracer();
```

OTel's own design guidance for libraries is *"only instrument your library at its own level"* and
*"When in doubt, don't instrument"*, with high-volume spans behind a configuration option disabled
by default. The spec goes further for application code: where Context is implicit, instrumentation
authors are *"discouraged from using the `Context` API directly"* and should go through
cross-cutting APIs instead.

**The seam already exists and is unused.** `PerConnectionHandler.handle(ConnectionContext)` and
the `WebSocketFrameHandler` callback interface are the boundaries. A decorating handler — the
"library instrumentation" pattern, a wrapper outside both the caller and the callee — is where
spans belong. That would leave `AbstractEndpointHandler` and `FreeNoteEndpoint` with no telemetry
import at all.

## 5. Span attributes are silently truncated

```java
// SpanLimitsConfig:9
.setMaxNumberOfAttributes(2)
```

Every `PersistenceManager` span sets **three** (`db.system`, `db.operation`, plus one of
`app.persistence.config` / `app.persistence.scheduler.active` / `app.persistence.mode`) —
`:41-43`, `:83-85`, `:100-102`, `:118-120`. `ConnectionPipeline.buildSpan` sets **four**. So one
to two attributes are dropped on every span, silently, with no warning. Anyone debugging "why is
this attribute missing from Jaeger" will lose an hour to a limit set in a different module.

## 6. Exporters: duplicate, synchronous, and unbatched on the hot path

```java
// SdkTracerProviderConfig:23-26
.addSpanProcessor(SpanProcessorConfig.simpleSpanProcessor(LoggingSpanExporter.create()))
.addSpanProcessor(SpanProcessorConfig.simpleSpanProcessor(SpanExporterConfig.otlpHttpSpanExporter(httpEndpoint)))
.addSpanProcessor(SpanProcessorConfig.batchSpanProcessor(SpanExporterConfig.otlpHttpSpanExporter(httpEndpoint)))
.addSpanProcessor(SimpleSpanProcessor.create(SpanExporterConfig.otlpGrpcSpanExporter(grpcEndpoint)))
```

- The **same OTLP/HTTP endpoint** is registered twice — once via `SimpleSpanProcessor`, once via
  `BatchSpanProcessor` — so every span is exported to it twice.
- Two `SimpleSpanProcessor`s export **inline on the calling thread**. On the message path that is
  a blocking network call per span, inside the code path being measured. A batch processor is the
  norm for anything on a request path.
- `BatchSpanProcessor` is configured with `maxQueueSize(50)` / `maxExportBatchSize(50)`
  (`SpanProcessorConfig:16-17`); when the queue fills, spans are dropped silently.
- `SamplerConfig.alwaysOn()` (`SdkTracerProviderConfig:27`) — no head or tail sampling. Combined
  with finding 1, nothing bounds volume.

## 7. The latency histogram is off by 1000×

`OtelLatencyMetric` declares seconds and second-scale buckets, then records milliseconds:

```java
@Builder.Default private String unit = "s";                       // :21
private static final List<Double> DURATION_SECONDS_BUCKETS = ...  // :23-26

public void record(double duration, TimeUnit unit) {
    this.doubleHistogram.record(duration);                        // :30 — unit ignored
}

public <T> T time(Supplier<T> action) {
    var startTimer = System.currentTimeMillis();
    var result = action.get();
    var endTimer = System.currentTimeMillis();
    record(endTimer - startTimer, TimeUnit.MILLISECONDS);         // :54 — ms into a seconds histogram
    return result;
}
```

`AbstractEndpointHandler:42` calls exactly this `time(Supplier)` overload on the production path.
`time(Runnable)` (`:59-68`) computes seconds correctly but then mislabels them `NANOSECONDS`, and
`stop(long)` (`:44-47`) also records milliseconds. Every `websocket.latency` observation is
therefore ~1000× too large, and the bucket boundaries (`0.005 … 10.0`) place every real request in
the top bucket.

## 8. Start-up ordering can silently no-op every span

```java
static { SAMPLE_GLOBAL_TELEMETRY = new SampleGlobalOpenTelemetry(); }   // :30-32

public SampleGlobalOpenTelemetry() {
    this.openTelemetry =
        GlobalOpenTelemetry.isSet() ? GlobalOpenTelemetry.get() : GlobalOpenTelemetry.getOrNoop();  // :36
}
```

If the class initialises before the app calls `GlobalOpenTelemetry.set(...)`, the singleton
captures the **no-op** implementation and every span for the process lifetime is discarded without
an error. Separately, `getTracer()` returns null unless `initProviders()` has run, so a call
before `init()` is an NPE rather than a silent no-op.

Two more class-load-order dependencies of the same kind: `MetricUtils:9` captures
`getSampleGlobalTelemetry().getMetricRegistries()` into a static field, and
`PersistenceManager:20-21` captures a tracer and a histogram. The three entry points
(`SimpleServer:18-19`, `core/startup/FreeNoteApplication:28-29`,
`freedraw/legacy/FreeNoteApplication:33-34`) do happen to order this correctly — `set(create())`
then `init()` — but nothing enforces it, and the failure mode is silent.

Also `SampleGlobalOpenTelemetry:37-40` builds `RuntimeTelemetry` into a local variable that is
immediately discarded.

## 9. Propagators configured, never used

`ContextPropagatorsConfig` correctly composes W3C Trace Context and W3C Baggage, and
`OpenTelemetrySdkConfig:22` installs them — but there is **no `TextMapPropagator` call anywhere in
production code**, so nothing ever injects or extracts. The configuration is inert.

The relevant practice is extract-at-entry / inject-at-exit. For WebSocket the only header-bearing
moment is the HTTP upgrade; afterwards there are no headers, so a carrier has to be a message
envelope field — which is what the `Setter`/`Getter` abstraction exists for.

## Sources

| Claim | Source |
|---|---|
| "Any span that is created MUST also be ended. This is the responsibility of the user." | [Trace API spec § Span lifetime](https://opentelemetry.io/docs/specs/otel/trace/api/#span-lifetime) |
| Creation MUST NOT activate the span; `End` MUST NOT inactivate it | [Trace API spec § Context interaction](https://opentelemetry.io/docs/specs/otel/trace/api/#context-interaction) |
| `ContextStorage` default "stores it in thread local"; `Scope.close()` failure "is an error and may cause memory leaks" | [Java API reference](https://opentelemetry.io/docs/languages/java/api/) |
| "Context is not intended for passing optional parameters to an API…"; strict-context checker | [`Context.java`](https://github.com/open-telemetry/opentelemetry-java/blob/main/context/src/main/java/io/opentelemetry/context/Context.java) |
| Instrumentation placement; "When in doubt, don't instrument" | [Instrumenting libraries](https://opentelemetry.io/docs/concepts/instrumentation/libraries/) |
| Extract at entry points, inject at exit points | [Java instrumentation ecosystem](https://opentelemetry.io/docs/languages/java/instrumentation/) |
| Application code discouraged from using the Context API directly | [Context spec](https://opentelemetry.io/docs/specs/otel/context/) |
| Virtual threads do not propagate parent context | [issue #11950](https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/11950), [commit 628136e](https://github.com/open-telemetry/opentelemetry-java-instrumentation/commit/628136e076e5fc351d5180a093df0cd6126a23f1) |
| No WebSocket semantic convention exists | `specs/semconv/messaging/websocket/` returns 404; WebSocket absent from the [messaging index](https://opentelemetry.io/docs/specs/semconv/messaging/) |

Deliberately excluded: the WebSocket-specific blog posts and third-party packages surfaced during
research. All were vendor content or unmaintained, and none is authoritative.

## Suggested order of work

1. **Span lifecycle** — end what you start, and decide connection-span vs message-span explicitly.
   The strict-context checker (`-Dio.opentelemetry.context.enableStrictContext=true`) turns this
   from a silent leak into a loud failure, which is the tightest feedback loop available.
2. **Delete `TracingContext`** and use `Context`/`Scope` properly — or remove tracing from the
   server core entirely until there is something worth tracing. A never-read carrier is worse
   than nothing.
3. **Move instrumentation behind the existing interfaces** (`PerConnectionHandler`,
   `WebSocketFrameHandler`) so the endpoint and domain stop importing telemetry.
4. **Fix the plumbing**: attribute limit, duplicate and synchronous exporters, sampling.
5. **Fix the histogram unit** — a one-line bug that makes the existing dashboards wrong.

## Verification

- `-Dio.opentelemetry.context.enableStrictContext=true` in tests and staging: catches scopes
  closed on the wrong thread or never closed. Documented only in the `Context.java` Javadoc.
- Confirm spans actually arrive. With four exporters configured it is hard to tell whether a
  missing span means "not created" or "dropped" — reduce to one batch processor before trusting
  any conclusion drawn from absence.
- After any change, re-run the k6 harness and confirm the latency histogram's bucket distribution
  moves from "everything in the top bucket" to something plausible.
- There is no WebSocket semantic convention to check span naming against, so record the chosen
  convention in an ADR, or the next person will re-litigate it.
