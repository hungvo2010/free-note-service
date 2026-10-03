# Mission: Instrumentation design — telemetry that stays out of the domain

## Why

I hand-rolled a WebSocket server (Java 21, no framework) and now need to instrument it.
I already wrote tracing code into it and got it wrong in a specific way: `ConnectionPipeline`
starts a span on every handshake and every message and never ends one, `TracingContext` is
built and passed into `ReadableContext` where nothing ever reads it, and `MetricUtils` is
called statically from inside `AbstractEndpointHandler`. My goal is to be able to instrument a
service — this one and the next one — so that the domain code contains no telemetry calls at
all, and to know where the boundaries belong instead of guessing.

## Success looks like

- I can say where a span starts and ends for a long-lived connection server, and defend that
  choice rather than reflexively doing one span per message.
- I can add a feature and instrument it without importing a telemetry type into the domain
  packages (`com.freedraw.entities`, `com.freedraw.service`).
- Given a situation, I can choose between a javaagent, library instrumentation, and manual
  calls, and say what each costs me.
- I can make context survive the thread boundaries this server actually has — virtual threads
  in `LegacyBootstrap`, a selector loop in `NIOServerBootstrap`, and NIO.2 completion handlers
  in `AsyncNIOServerSession`.
- I can explain why my current `TracingContext` is dead weight and what a correct version of
  that same idea would be.

## Constraints

- No framework: no Spring, no servlet container, no Netty. Whatever I learn has to work on a
  raw `ServerSocketChannel` and a hand-written HTTP upgrade.
- Changes land in a working codebase with tests and a git history I care about keeping clean —
  small and reversible beats comprehensive.
- Learning happens alongside architecture work already in flight, so lessons need to be short
  and finishable.

## Out of scope

- Building a full observability platform: dashboards, alerting, SLOs, sampling strategy at scale.
- Backend or vendor selection (Grafana vs Jaeger vs hosted).
- Distributed tracing across processes. The server has Redis pub/sub for sticky routing, but
  this mission is one process.
- Metrics and log-correlation design. Deferred, not dismissed — revisit as a separate
  workspace if it becomes the bottleneck.
