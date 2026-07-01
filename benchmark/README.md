# Benchmarking: NIO Selector vs Virtual Thread

This directory benchmarks the two WebSocket server implementations in this repo
under identical load and captures the metrics that actually separate them —
especially **memory per connection** and **echo latency**.

| Server   | Entry point | Threading model |
|----------|-------------|-----------------|
| `vthread` | `com.freenote.app.server.core.legacy.launcher.SimpleServer` | `Executors.newVirtualThreadPerTaskExecutor()` — **1 virtual thread per connection** |
| `nio`     | `com.freenote.app.server.core.nio.launcher.nio.NIOSimpleServer` | `Executors.newFixedThreadPool(2)` + a single `Selector` loop — **O(1) threads** |

Both expose the same `/echo` WebSocket route (echoes text frames, answers pings).

## Prerequisites

- **JDK 21** (already the project toolchain — see `build.gradle`).
- **k6** (WebSocket support is built in): `brew install k6`
- `lsof` (preinstalled on macOS).

## Quick start

```bash
# from repo root

# 1) Memory per connection: hold 1000 idle sessions for 60s
MODE=idle TARGET_VUS=1000 HOLD_SEC=60 ./benchmark/run-benchmark.sh

# 2) Throughput / latency: 200 VUs each sending 200 echo requests
MODE=throughput TARGET_VUS=200 MSGS_PER_VU=200 HOLD_SEC=30 ./benchmark/run-benchmark.sh
```

The script builds the fat jar (`./gradlew shadowJar`) if needed, starts each
server in its own JVM with identical flags, runs k6 against it, captures memory,
and prints a comparison table. Raw artifacts land in
`benchmark/results/<label>-<mode>-<timestamp>/`.

## What each mode measures

### `idle` — the memory test (the one that matters for NIO vs vthread)
Each VU opens one WebSocket and holds it open (pings every 3s) for `HOLD_SEC`.
At steady state you have `TARGET_VUS` live connections. Capture, then divide by
the connection count:

- **`rss_per_conn_kb`** — process RSS / connections. The headline number.
- **`platform_threads`** — from `jcmd Thread.print`. NIO stays ~constant; vthread
  grows ~1 per connection (virtual threads appear in the dump in JDK 21+).
- **`heap_committed_kb`** — from `jcmd VM.native_memory summary`. Virtual-thread
  stacks are heap-allocated, so vthread heap grows with connection count; NIO
  heap stays roughly flat (state machine + reusable buffers).

> Expect: NIO `rss_per_conn_kb` and `platform_threads` roughly **flat** as
> `TARGET_VUS` rises; vthread `platform_threads` ~ `TARGET_VUS` and
> `rss_per_conn_kb` a few KB each. That gap *is* the memory comparison.

### `throughput` — the latency / throughput test
Each VU opens a session, fires `MSGS_PER_VU` echo requests, and the script
records each round-trip in the `echo_rtt_ms` Trend. Look in `k6-summary.json`:

- `echo_rtt_ms{p(50),p(95),p(99)}` — echo latency under load.
- `ws_msgs_received` rate — messages/sec sustained.
- `ws_session_duration` — handshake-to-close cost.

## Files written per run

```
benchmark/results/<label>-<mode>-<timestamp>/
├── server.stdout / server.stderr   # server console
├── nmt.baseline.txt / nmt.peak.txt # jcmd VM.native_memory summary (Thread/Heap/Internal)
├── ps.baseline.txt / ps.peak.txt   # RSS / VSZ (KB)
├── threads.peak.txt                # jcmd Thread.print (count lines = thread count)
├── heap.peak.txt / gc.peak.txt     # jcmd GC.heap_info + jstat -gc
├── k6-summary.json                 # all k6 metrics (parse for p95/p99)
├── k6.stdout / k6.stderr           # full k6 run log
└── summary.tsv                     # one-row tidy summary used by the script
```

## Controls (why the comparison is fair)

The script holds everything constant **except** the server implementation:

- **Identical JVM flags** for both: fixed `-Xms/-Xmx` (default 512m), `UseZGC`,
  `NativeMemoryTracking=detail`.
- **Identical load** from the same k6 script (`benchmark/k6-comparison.js`).
- **Logging silenced** via `benchmark/log4j2-bench.xml` (Root = ERROR, file-only,
  no synchronized `ConsoleAppender`). This matters: per-message INFO logging
  would otherwise dominate CPU, and as documented in
  `docs/VIRTUAL_THREAD_LOGGING_ANALYSIS.md`, the synchronized console appender
  **pins virtual-thread carriers** in JDK 21 — which would make vthread look
  artificially worse. Set `Root level="info"` in the bench config if you
  specifically want to measure *with* logging overhead.

## Confounds to keep in mind

1. **Different handler pipelines.** The two servers do not share a handler:
   `vthread` runs `DefaultLegacySessionBasedConnectionHandler`; `nio` runs
   `ConnectionPipeline` + `NIOIncomingSocketHandler`, which also creates an
   OpenTelemetry span per event. So this is an **end-to-end** comparison of the
   two servers, not a pure isolation of the threading model. To isolate *only*
   the I/O model, run the same handler behind both bootstraps (or disable OTel
   on the NIO path) and re-run.
2. **Warmup.** The script waits 5s after start before the baseline snapshot and
   uses a 15s k6 ramp-up. For tighter numbers, raise `HOLD_SEC` and run twice,
   discarding the first (cold) run.
3. **GC choice.** ZGC keeps pauses low; to study heap headroom instead, switch
   the script's `-XX:+UseZGC` to `-XX:+UseG1GC` and watch `gc.peak.txt`.
4. **OS buffers / TIME_WAIT.** At very high connection churn, ephemeral port
   exhaustion or `TIME_WAIT` can bottleneck the *client* side. The `idle` mode
   (long-lived connections) avoids this; `throughput` mode reuses sessions to
   limit churn.
5. **Fixed heap ceiling.** If vthread hits `-Xmx` under high connection counts,
   it will GC thrash or OOM. Raise `HEAP=1g` and re-run to find the steady-state
   cost rather than the OOM cliff.

## Going deeper

- **JFR (allocation/GC/hotspots):** add to `JVM_FLAGS`
  `-XX:StartFlightRecording=filename=/tmp/<label>.jfr,settings=profile,duration=120s`
  then open the `.jfr` in JDK Mission Control.
- **Native memory diff:** `jcmd <pid> VM.native_memory baseline` before load,
  `jcmd <pid> VM.native_memory detail.diff` after — shows exactly what grew.
- **Per-connection heap (vthread):** the virtual-thread stack segments show up
  in the `Java Heap` / `Other` areas of the NMT diff, not in the `Thread`
  (platform stack) area.

## Simulating real network conditions

The `idle` / `throughput` modes above assume well-behaved clients on a clean
network. Real traffic is messier: users type slowly, go idle, burst, drop in
and out, and sit behind flaky mobile links. This directory adds **two layers**
of realism so you can find out whether the server actually survives.

### Layer 1 — application-level client chaos (`k6-chaos.js`)

Simulates *client behaviour* with no extra dependencies (just k6). Pick a
scenario with `--env SCENARIO`:

| Scenario | What it models | What it stresses |
|----------|----------------|------------------|
| `slow` | Users that send a message every 1–10s (typing, low engagement) | Long-lived idle connections, ping/pong keep-alive |
| `stall` | A few quick messages, then a long silence (pings only), then resume | Idle → active transitions, partial buffer state |
| `burst` | Long idle, then 50 messages back-to-back | Backpressure, write-buffer fill, frame parsing under load |
| `churn` | Rapid connect → 1 message → disconnect, looped | Connection setup/teardown, `TIME_WAIT` exhaustion, selector key churn |
| `mixed` | A blend of the above spread across VUs | Heterogeneous population — closest to real traffic |

```bash
# slow, disengaged users
SERVER=nio SCENARIO=slow TARGET_VUS=500 HOLD_SEC=120 ./benchmark/run-chaos.sh

# connection churn
SERVER=vthread SCENARIO=churn TARGET_VUS=200 HOLD_SEC=60 ./benchmark/run-chaos.sh

# a bit of everything
SERVER=nio SCENARIO=mixed TARGET_VUS=300 HOLD_SEC=90 ./benchmark/run-chaos.sh
```

Watch in `k6-summary.json`: `chaos_rtt_ms` (p50/p95/p99), and especially
**`chaos_msgs_sent` vs `chaos_msgs_received`** — a gap means the server dropped
or stalled under chaos. `chaos_disconnects` and `chaos_handshake_failed` show
connection-level failures.

### Layer 2 — network-level shaping (Toxiproxy)

Simulates *the wire* between client and server: latency, jitter, bandwidth
limits, slow close, mid-stream timeouts, and partial reads. Toxiproxy runs in
Docker and intercepts the TCP stream. `run-chaos.sh` starts it automatically
when `TOXIPROXY=docker` (default).

| Toxic env var | Effect | Good for finding |
|---------------|--------|------------------|
| `LATENCY_MS` (+ `JITTER_MS`) | Adds delay (±jitter) downstream | Latency-sensitive protocols, timeout bugs |
| `BANDWIDTH_KB` | Caps throughput (e.g. `256` = 256 KB/s) | Large-frame handling, write-buffer stalls |
| `SLOW_CLOSE_MS` | Delays the FIN/close handshake | Half-open connections, close-path leaks |
| `TIMEOUT_MS` | Drops the connection after N ms of inactivity | Idle-connection cleanup, ping/pong correctness |
| `LIMIT_DATA_KB` | Closes the connection after N KB transferred | Long-message handling, resource cleanup |
| `SLICER=1` (+ `SLICER_SIZE`, `SLICER_DELAY_MS`) | Splits the stream into small chunks with delays | **Partial reads** — frames split across TCP segments |

```bash
# 200ms latency + 256KB/s bandwidth, slow users
SERVER=nio SCENARIO=slow LATENCY_MS=200 JITTER_MS=50 BANDWIDTH_KB=256 \
    TARGET_VUS=300 HOLD_SEC=90 ./benchmark/run-chaos.sh

# 5s idle timeout — does the server drop idle sockets cleanly?
SERVER=vthread SCENARIO=stall TIMEOUT_MS=5000 ./benchmark/run-chaos.sh

# slow close + churn — tests the close path under load
SERVER=nio SCENARIO=churn SLOW_CLOSE_MS=2000 ./benchmark/run-chaos.sh

# slicer (1KB chunks, 10ms apart) — the partial-read stress test
SERVER=nio SCENARIO=burst SLICER=1 SLICER_SIZE=1024 SLICER_DELAY_MS=10 \
    ./benchmark/run-chaos.sh
```


### The orchestrator (`run-chaos.sh`)

`run-chaos.sh` ties both layers together so a single command produces a full
chaos run against either server:

1. Starts Toxiproxy (Docker) and creates the proxy `localhost:18189 → host.docker.internal:8189`.
2. Applies any toxics you set via env vars.
3. Builds the fat jar if missing, starts the chosen server (`nio` or `vthread`) with fixed heap + NMT.
4. Runs `k6-chaos.js` against the proxied URL (so traffic flows client → Toxiproxy → server).
5. Captures peak RSS, NMT, and a thread dump, then tears the server down and resets toxics.

Set `TOXIPROXY=none` to bypass the proxy and hit the server directly (use this
to isolate app-level chaos from network shaping). Set `TOXIPROXY=host` if you
run Toxiproxy via `brew install toxiproxy` instead of Docker.

Manual control (proxy stays up across runs, so you can tweak conditions live):

```bash
docker compose -f benchmark/docker-compose.toxiproxy.yml up -d
UPSTREAM=host.docker.internal:8189 ./benchmark/toxiproxy-control.sh create
LATENCY_MS=300 ./benchmark/toxiproxy-control.sh add      # add a toxic
./benchmark/toxiproxy-control.sh reset                    # remove all toxics
./benchmark/toxiproxy-control.sh status                   # show proxy + toxics
```

### What to look for (and why it matters for *your* servers)

The point of chaos testing is to expose the gaps that a clean benchmark hides.
For the two implementations in this repo, the scenarios above target specific
known-weak spots:

- **`SLICER=1` (partial reads) vs the NIO server** — this is the harshest test
  for `NIOServerSession`. `readFromChannel()` does `byteBuffer.clear()` then
  `channel.read()` into a fixed 2 KB direct buffer. If a WebSocket frame
  arrives split across TCP segments (exactly what the slicer forces), the
  parser must reassemble across reads. Watch for `chaos_msgs_received` <
  `chaos_msgs_sent` (lost/malformed frames) or a rise in `chaos_disconnects`.
- **`SLOW_CLOSE_MS` / `TIMEOUT_MS` vs the NIO write path** —
  `NIONetworkRequestData.write()` loops `channel.write()` synchronously on the
  selector thread. A slow-draining client (bandwidth cap + slow close) can make
  that loop spin and stall *every other connection* on the single selector
  thread. Compare `chaos_rtt_ms` p95/p99 between a clean run and a
  `BANDWIDTH_KB=128` run — a blow-up there means the reactor is being blocked.
- **`churn` vs `ConcurrentHashMap` cleanup** — rapid connect/disconnect
  exercises `channelData.remove()` and `key.cancel()` in `cleanupChannel()`.
  If state leaks, peak RSS will climb across repeated churn runs (run it twice
  in a row and compare `ps.peak.txt`).
- **`stall` + `TIMEOUT_MS` vs idle connection lifecycle** — a long silent
  period followed by a forced drop checks that `MessageState.transition()`
  returning `null` actually triggers full cleanup (key cancel + map remove +
  channel close) and doesn't leave the connection half-open.

### Files

| File | Purpose |
|------|---------|
| `k6-chaos.js` | k6 chaos scenarios: slow / stall / burst / churn / mixed |
| `docker-compose.toxiproxy.yml` | Toxiproxy container (API :8474, proxy :18189) |
| `toxiproxy-control.sh` | curl-based control: create proxy, add/reset toxics, status |
| `run-chaos.sh` | Orchestrator: Toxiproxy + server + k6 + memory capture |

### Prerequisites

- k6 (already required above): `brew install k6`
- Docker (for Toxiproxy): `brew install --cask docker` — or `brew install toxiproxy` and use `TOXIPROXY=host`
- `lsof` (preinstalled on macOS)

