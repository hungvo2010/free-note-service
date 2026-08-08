#!/usr/bin/env bash
#
# run-chaos.sh — drive a server under simulated real-world conditions:
#   * application-level client chaos   (k6-chaos.js: slow/stall/burst/churn)
#   * network-level shaping            (Toxiproxy: latency/bandwidth/loss...)
#
# Quick examples (from repo root):
#   SERVER=nio SCENARIO=slow LATENCY_MS=200 BANDWIDTH_KB=256 ./benchmark/run-chaos.sh
#   SERVER=vthread SCENARIO=churn TIMEOUT_MS=5000 ./benchmark/run-chaos.sh
#   SERVER=nio SCENARIO=mixed LATENCY_MS=100 JITTER_MS=40 SLOW_CLOSE_MS=2000 ./benchmark/run-chaos.sh
#
# Env knobs (defaults):
#   SERVER     vthread | nio             (nio)
#   SCENARIO   slow | stall | burst | churn | mixed   (slow)
#   TOXIPROXY  docker | host | none      (docker)  'host' = brew toxiproxy already running
#   TARGET_VUS, HOLD_SEC, HEAP, PORT     (100 / 60 / 512m / 8189)
#   Toxics (any/all optional): LATENCY_MS, JITTER_MS, BANDWIDTH_KB,
#     SLOW_CLOSE_MS, TIMEOUT_MS, LIMIT_DATA_KB, SLICER, SLICER_SIZE, SLICER_DELAY_MS
#
set -euo pipefail

SCENARIO="${SCENARIO:-slow}"
TARGET_VUS="${TARGET_VUS:-100}"
HOLD_SEC="${HOLD_SEC:-60}"
SERVER="${SERVER:-nio}"
TOXIPROXY="${TOXIPROXY:-docker}"
HEAP="${HEAP:-512m}"
PORT="${PORT:-8189}"
K6="${K6:-k6}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BENCH="$ROOT/benchmark"
RESULTS_DIR="${RESULTS_DIR:-$BENCH/results}"
STAMP="$(date +%Y%m%d-%H%M%S)"
OUT="$RESULTS_DIR/chaos-${SERVER}-${SCENARIO}-${STAMP}"
mkdir -p "$OUT"

JAVA="${JAVA_HOME:-}/bin/java"; [[ -x "$JAVA" ]] || JAVA=java
JCMD="${JAVA_HOME:-}/bin/jcmd"; [[ -x "$JCMD" ]] || JCMD=jcmd

case "$SERVER" in
    vthread) MAIN="com.freenote.app.server.core.legacy.launcher.SimpleServer" ;;
    nio)     MAIN="com.freenote.app.server.core.nio.launcher.nio.NIOSimpleServer" ;;
    *) echo "SERVER must be 'vthread' or 'nio'" >&2; exit 1 ;;
esac

# --- fat jar (build if missing) ---
JAR="$(ls "$ROOT"/build/libs/*-all.jar 2>/dev/null | head -1 || true)"
if [[ -z "$JAR" ]]; then
    echo ">> building fat jar..."; (cd "$ROOT" && ./gradlew shadowJar -x test -q)
    JAR="$(ls "$ROOT"/build/libs/*-all.jar | head -1)"
fi

# --- toxiproxy ---
PROXY_PORT=18189
if [[ "$TOXIPROXY" == "docker" ]]; then
    echo ">> starting toxiproxy (docker)..."
    docker compose -f "$BENCH/docker-compose.toxiproxy.yml" up -d >/dev/null 2>&1 || true
    sleep 2
    UPSTREAM="host.docker.internal:$PORT" PROXY_NAME=freenote LISTEN_PORT=$PROXY_PORT "$BENCH/toxiproxy-control.sh" create
    TARGET_URL="ws://localhost:$PROXY_PORT/echo"
elif [[ "$TOXIPROXY" == "host" ]]; then
    UPSTREAM="localhost:$PORT" PROXY_NAME=freenote LISTEN_PORT=$PROXY_PORT "$BENCH/toxiproxy-control.sh" create
    TARGET_URL="ws://localhost:$PROXY_PORT/echo"
else
    TARGET_URL="ws://localhost:$PORT/echo"
fi

# apply toxics only if at least one is set
if [[ "$TOXIPROXY" != "none" && -n "${LATENCY_MS:-}${BANDWIDTH_KB:-}${SLOW_CLOSE_MS:-}${TIMEOUT_MS:-}${LIMIT_DATA_KB:-}${SLICER:-}" ]]; then
    "$BENCH/toxiproxy-control.sh" add
fi
echo ">> target url: $TARGET_URL"

# --- start server ---
"$JAVA" -Xms"$HEAP" -Xmx"$HEAP" -XX:+UseZGC -XX:NativeMemoryTracking=detail \
    -Dlog4j.configurationFile=file:"$BENCH/log4j2-bench.xml" \
    -cp "$JAR" "$MAIN" "$PORT" >"$OUT/server.stdout" 2>"$OUT/server.stderr" &
PID=$!
echo ">> started $SERVER  pid=$PID  port=$PORT"
for _ in $(seq 1 60); do lsof -nP -iTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1 && break; sleep 0.5; done
sleep 5  # warmup

echo ">> baseline memory"
ps -o rss=,vsz= -p "$PID" >"$OUT/ps.baseline.txt" 2>/dev/null || true
"$JCMD" "$PID" VM.native_memory summary >"$OUT/nmt.baseline.txt" 2>&1 || true

echo ">> running k6 chaos  scenario=$SCENARIO  vus=$TARGET_VUS  hold=${HOLD_SEC}s"
"$K6" run "$BENCH/k6-chaos.js" \
    --env SCENARIO="$SCENARIO" --env TARGET_VUS="$TARGET_VUS" --env HOLD_SEC="$HOLD_SEC" \
    --env URL="$TARGET_URL" \
    --summary-export "$OUT/k6-summary.json" \
    >"$OUT/k6.stdout" 2>"$OUT/k6.stderr" || true

echo ">> peak memory + threads"
ps -o rss=,vsz= -p "$PID" >"$OUT/ps.peak.txt" 2>/dev/null || true
"$JCMD" "$PID" VM.native_memory summary >"$OUT/nmt.peak.txt" 2>&1 || true
"$JCMD" "$PID" Thread.print >"$OUT/threads.peak.txt" 2>&1 || true

echo ">> stopping server"
kill "$PID" 2>/dev/null || true; wait "$PID" 2>/dev/null || true

[[ "$TOXIPROXY" != "none" ]] && "$BENCH/toxiproxy-control.sh" reset || true

RSS=$(awk '{print $1}' "$OUT/ps.peak.txt" 2>/dev/null || echo 0)
NTH=$(grep -c 'java.lang.Thread.State' "$OUT/threads.peak.txt" 2>/dev/null || echo 0)

echo
echo "==================== CHAOS RUN DONE ===================="
echo "Server: $SERVER   Scenario: $SCENARIO   Toxiproxy: $TOXIPROXY"
echo "Toxics: LATENCY_MS=${LATENCY_MS:-} JITTER_MS=${JITTER_MS:-} BANDWIDTH_KB=${BANDWIDTH_KB:-} SLOW_CLOSE_MS=${SLOW_CLOSE_MS:-} TIMEOUT_MS=${TIMEOUT_MS:-} LIMIT_DATA_KB=${LIMIT_DATA_KB:-} SLICER=${SLICER:-}"
echo "Peak RSS=${RSS}KB   platform_threads=${NTH}"
echo "Results: $OUT"
echo
echo "Look in k6-summary.json for: chaos_rtt_ms (p50/p95/p99),"
echo "chaos_msgs_sent vs chaos_msgs_received (loss under chaos),"
echo "chaos_disconnects, and chaos_handshake_failed rate."
