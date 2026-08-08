# Why Legacy VT Latency Is Higher Than Async NIO.2 Under Load

## Setup

| | SimpleServer (Legacy VT) | ModernEchoServer (Async NIO.2) |
|---|---|---|
| I/O model | Blocking `InputStream.read()` | `AsynchronousSocketChannel` + OS callbacks |
| Connection model | 1 virtual thread per connection | 0 threads per connection |
| Threads under 1k conns | ~1000 VTs + ~10 carriers + main | ~2 async channel group threads |

## Root Cause: Carrier Thread Starvation

The Legacy server creates **1000 virtual threads** but the JVM only has **~10 carrier threads** (ForkJoinPool parallelism = CPU cores). Every virtual thread must be *mounted* on a carrier to execute. When all carriers are busy, virtual threads queue up — that queue time is the added latency.

### Legacy VT request path (4 mount/unmount cycles per message)

```
accept() → VT created
  → mount on carrier
    → socket.read() blocks → unmount from carrier
  → data arrives
    → mount on carrier
      → parse HTTP upgrade
      → socket.write() blocks → unmount from carrier
    → write completes
      → mount on carrier
        → echo loop: socket.read() blocks → unmount from carrier
      → message arrives
        → mount on carrier
          → parse frame, echo back
          → socket.write() → unmount from carrier
```

Each I/O operation causes the VT to unmount (stack saved to heap) and later remount (stack restored from heap). Under 1k concurrent connections all hitting at once, the 10 carrier threads can't keep up — VTs pile up in the scheduler queue.

### Async NIO.2 request path (0 mount/unmount)

```
OS accept → callback fires
  → async read submitted → returns immediately
OS read completes → callback fires on async channel group thread
  → parse HTTP upgrade
  → async write submitted → returns immediately
OS write completes → callback fires
  → async read submitted for next message
OS read completes → callback fires
  → parse frame, echo back
  → async write submitted
```

No thread-per-connection. No mount/unmount. The OS multiplexes I/O via kqueue (macOS) / epoll (Linux). Callbacks run on a small shared thread pool — 1k connections share it without queuing.

## Secondary Factor: Sequential `accept()`

```java
// LegacyBootstrap — one connection at a time
while (!serverSocket.isClosed()) {
    var socket = serverSocket.accept();  // sequential
    virtualExecutorService.submit(...);
}
```

Vs Async where `asyncServerChannel.accept(null, handler)` re-registers immediately — the OS can accept many connections in parallel.

## How to Verify in Grafana

| Metric | Legacy VT (9464) | Async NIO.2 (9465) |
|---|---|---|
| `jvm_threads_virtual` | ≈ concurrent users | ≈ 0 |
| `jvm_threads_platform` | ~10 (flat) | ~5 (flat) |
| `websocket_latency_milliseconds` p99 | higher (carrier queue) | lower (direct callback) |
| `jvm_threads_state{state="timed_waiting"}` | high (idle VTs) | low |

## Mitigation for Legacy VT

- Increase carrier parallelism: `-Djdk.virtualThreadScheduler.parallelism=100`
- Use `--rate` in k6 to ramp connections gradually instead of all-at-once (`constant-vus`)
- For latency-sensitive workloads, the async model (NIO.2) is the better choice — VT excels at *throughput* with simple code, not tail latency under burst
