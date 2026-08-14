# free-note-service

A WebSocket server built **from scratch on Java Core** — no Netty, no Spring, no framework. The full RFC 6455 protocol stack (handshake, frame parsing, fragmentation, heartbeat) is implemented by hand, and the server ships in **three I/O models** that are benchmarked against each other to answer a concrete question: *which threading model actually scales?*

## Highlights

- **Pure-Java WebSocket stack** — hand-rolled HTTP upgrade + RFC 6455 frame parser, fragmentation reassembly, control frames (ping/pong/close), and endpoint routing via custom annotations.
- **Three I/O models, one codebase** — blocking I/O + virtual threads, a single-thread NIO `Selector` (reactor), and async NIO.2. All exposed through a transport-agnostic `NetworkRequestData` abstraction so the protocol layer doesn't care how bytes arrive.
- **Real, reproducible benchmarks** — k6 load tests comparing memory per connection (RSS + NMT) and p99 echo latency across models, run through identical JVM flags and identical traffic.
- **Chaos testing** — k6 client-behavior scenarios (slow / stall / burst / churn / mixed) layered with Toxiproxy network shaping (latency, jitter, bandwidth caps, stream slicing for partial reads, slow close).
- **Compile-time dependency injection** — a custom Java annotation processor (`UtilityProcessor`) that generates DI/bean wiring, endpoint registration, and factory code from `@Singleton`, `@Factory`, and `@WebSocketEndpoint` annotations.
- **First-party observability** — OpenTelemetry spans per handshake/message, Prometheus + Grafana dashboard that compares the I/O models side by side, Jaeger tracing, virtual-thread tracking, and JVM thread-state metrics.
- **Spec-driven development** — architecture captured in OpenSpec specs (transport abstraction, connection pipeline, frame handler, etc.).

## Architecture

```
                    ┌─────────────────────────────────────────────┐
   TCP bytes  ───▶  │  Transport layer (pluggable)                │
                    │   • Blocking  + virtual threads             │
                    │   • NIO Selector (reactor, O(1) threads)    │
                    │   • Async NIO.2 (OS callbacks)              │
                    └──────────────────┬──────────────────────────┘
                                       │ NetworkRequestData (abstraction)
                    ┌──────────────────▼──────────────────────────┐
                    │  ConnectionPipeline (state machine)         │
                    │   HandShakeState → MessageState → close     │
                    └──────────────────┬──────────────────────────┘
                                       │
                    ┌──────────────────▼──────────────────────────┐
                    │  WebSocket protocol layer                   │
                    │   frame parser, fragmentation, control      │
                    │   frames, endpoint dispatch                 │
                    └─────────────────────────────────────────────┘
```

- **`core/`** — servers, bootstraps, and transport abstractions (legacy, NIO, async NIO).
- **`frames/`** — RFC 6455 frame model, parsing, fragmentation, factories.
- **`parser/`** — HTTP upgrade + WebSocket frame parsers over `InputStream` / `ByteBuffer` / async buffers.
- **`routes/`** — endpoint abstraction and `@WebSocketEndpoint`-based dispatch.
- **`auth/`** — Sec-WebSocket-Key handshake acceptance.
- **`UtilityProcessor/`** — the compile-time annotation processor (DI + endpoint discovery).
- **`observability/`** — OpenTelemetry SDK wiring, metrics, virtual-thread tracking.
- **`free-draw/`** — the reference application (collaborative-drawing endpoints) on top of the server.
- **`benchmark/`** — k6 + Toxiproxy load/chaos harness.

## Feature status

- [x] WebSocket handshaking (RFC 6455 upgrade + key acceptance)
- [x] Three I/O models: blocking + virtual threads, NIO Selector, async NIO.2
- [x] Fragmentation (continuation frames, large-message reassembly)
- [x] Heartbeat (ping/pong keep-alive) and close handshake
- [x] HTTPS/TLS support (SSL/TLS servers + keystores)
- [x] Custom DI + endpoint routing via annotation processing
- [x] Observability (OpenTelemetry, Prometheus, Grafana, Jaeger)
- [x] Benchmark + chaos testing harness (k6, Toxiproxy)
- [ ] Compression (`permessage-deflate` extension negotiation parsed; frame compression not yet implemented)

## Key results worth knowing

- **NIO selector holds memory flat** as connections scale — `rss_per_conn_kb` and platform threads stay ~constant, while the virtual-thread server grows a thread (and heap-resident stack) per connection.
- **Virtual threads beat NIO on throughput, lose on tail latency under burst** — ~10 carrier threads queue 1k virtual threads under a traffic spike; the async NIO.2 path (0 mount/unmount) keeps p99 latency lower. Root cause documented in [`docs/vt-vs-nio2-latency.md`](docs/vt-vs-nio2-latency.md).
- Full measurement methodology and controls (identical JVM flags, silenced logging to avoid carrier pinning) in [`benchmark/README.md`](benchmark/README.md).

## Tech stack

Java 21 · Gradle · NIO/NIO.2 · virtual threads (Project Loom) · Java annotation processing · OpenTelemetry · Prometheus · Grafana · Jaeger · k6 · Toxiproxy · Docker Compose · Redis · Apicurio Schema Registry · SpotBugs · JaCoCo
