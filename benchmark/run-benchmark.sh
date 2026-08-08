#!/usr/bin/env bash
#
# run-benchmark.sh — compare the NIO-selector server against the virtual-thread
# server under identical load, capturing memory + thread + latency metrics.
#
# Usage:
#   MODE=idle TARGET_VUS=1000 HOLD_SEC=60  ./benchmark/run-benchmark.sh
#   MODE=throughput TARGET_VUS=200 MSGS_PER_VU=200 HOLD_SEC=30 ./benchmark/run-benchmark.sh
#
# Env knobs (defaults):
#   MODE          idle | throughput        (idle)
#   TARGET_VUS    concurrent VUs           (200)
#   HOLD_SEC      seconds at target VUs    (60)
#   MSGS_PER_VU   echo requests/VU         (100, throughput mode)
#   HEAP          -Xms/-Xmx                (512m)
#   PORT          server port              (8189)
#   K6            k6 binary                (k6)
#
set -euo pipefail

# ----------------------------- config --------------------------------------
MODE="${MODE:-idle}"
TARGET_VUS="${TARGET_VUS:-200}"
HOLD_SEC="${HOLD_SEC:-60}"
MSGS_PER_VU="${MSGS_PER_VU:-100}"
HEAP="${HEAP:-512m}"
PORT="${PORT:-8189}"
K6="${K6:-k6}"
RESULTS_DIR="${RESULTS_DIR:-$(cd "$(dirname "$0")" && pwd)/results}"

# ----------------------------- tooling -------------------------------------
if [[ -n "${JAVA_HOME:-}" ]]; then
    JAVA="${JAVA_HOME}/bin/java"
    JCMD="${JAVA_HOME}/bin/jcmd"
    JSTAT="${JAVA_HOME}/bin/jstat"
else
    JAVA="java"
    JCMD="jcmd"
    JSTAT="jstat"
fi

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOG_CFG="$ROOT/benchmark/log4j2-bench.xml"

# ----------------------------- build ---------------------------------------
JAR="$(ls "$ROOT"/build/libs/*-all.jar 2>/dev/null | head -1 || true)"
if [[ -z "$JAR" ]]; then
    echo ">> No fat jar found; building with ./gradlew shadowJar ..."
    (cd "$ROOT" && ./gradlew shadowJar -x test -q)
    JAR="$(ls "$ROOT"/build/libs/*-all.jar | head -1)"
fi
echo ">> Fat jar: $JAR"

mkdir -p "$RESULTS_DIR"
STAMP="$(date +%Y%m%d-%H%M%S)"

# label|main-class  (label is used in output filenames)
SERVERS=(
    "vthread|com.freenote.app.server.core.legacy.launcher.SimpleServer"
    "nio|com.freenote.app.server.core.nio.launcher.nio.NIOSimpleServer"
)

# JVM flags applied identically to BOTH servers (fixed heap, NMT, quiet logging).
JVM_FLAGS=(
    "-Xms${HEAP}" "-Xmx${HEAP}"
    "-XX:+UseZGC"
    "-XX:NativeMemoryTracking=detail"
    "-Dlog4j.configurationFile=file:${LOG_CFG}"
)

# ----------------------------- helpers -------------------------------------
wait_for_port() {
    local port="$1"
    for _ in $(seq 1 60); do
        if lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then return 0; fi
        sleep 0.5
    done
    return 1
}

wait_port_free() {
    local port="$1"
    for _ in $(seq 1 60); do
        if ! lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then return 0; fi
        sleep 0.5
    done
    return 1
}

# ----------------------------- per-server run ------------------------------
run_one() {
    local label="$1" mainClass="$2"
    local out="$RESULTS_DIR/${label}-${MODE}-${STAMP}"
    mkdir -p "$out"

    echo ""
    echo "==================== $label ($MODE) ===================="

    "$JAVA" "${JVM_FLAGS[@]}" -cp "$JAR" "$mainClass" "$PORT" \
        >"$out/server.stdout" 2>"$out/server.stderr" &
    local pid=$!
    echo ">> started $label  pid=$pid  port=$PORT"

    if ! wait_for_port "$PORT"; then
        echo "!! $label did not start; see $out/server.stderr"
        kill "$pid" 2>/dev/null || true
        return 1
    fi

    sleep 5   # let the JVM warm up / JIT a little before baseline
    echo ">> baseline memory (after warmup)"
    "$JCMD" "$pid" VM.native_memory summary >"$out/nmt.baseline.txt" 2>&1 || true
    ps -o rss=,vsz= -p "$pid" >"$out/ps.baseline.txt" 2>/dev/null || true

    echo ">> running k6  (vus=$TARGET_VUS  hold=${HOLD_SEC}s  mode=$MODE)"
    "$K6" run "$ROOT/benchmark/k6-comparison.js" \
        --env MODE="$MODE" \
        --env TARGET_VUS="$TARGET_VUS" \
        --env HOLD_SEC="$HOLD_SEC" \
        --env MSGS_PER_VU="$MSGS_PER_VU" \
        --env URL="ws://localhost:${PORT}/echo" \
        --summary-export "$out/k6-summary.json" \
        >"$out/k6.stdout" 2>"$out/k6.stderr" || true

    echo ">> peak memory (right after load)"
    "$JCMD" "$pid" VM.native_memory summary >"$out/nmt.peak.txt" 2>&1 || true
    ps -o rss=,vsz= -p "$pid" >"$out/ps.peak.txt" 2>/dev/null || true
    "$JCMD" "$pid" Thread.print      >"$out/threads.peak.txt" 2>&1 || true
    "$JCMD" "$pid" GC.heap_info      >"$out/heap.peak.txt"    2>&1 || true
    "$JSTAT" -gc "$pid"              >"$out/gc.peak.txt"      2>&1 || true

    echo ">> stopping $label"
    kill "$pid" 2>/dev/null || true
    wait "$pid" 2>/dev/null || true
    wait_port_free "$PORT" || true

    # ---- extract key numbers into a tidy TSV --------------------------------
    local rss_peak nthreads heap_used
    rss_peak=$(awk '{print $1}' "$out/ps.peak.txt" 2>/dev/null || echo 0)
    nthreads=$(grep -c 'java.lang.Thread.State' "$out/threads.peak.txt" 2>/dev/null || echo 0)
    # NMT "Java Heap" committed (KB): line like  ,  Java Heap (reserved=..., committed=12345KB)
    heap_used=$(grep -iE '^\s*,?\s*Java Heap' "$out/nmt.peak.txt" \
        | grep -oE 'committed=[0-9]+KB' | grep -oE '[0-9]+' | head -1 || echo 0)

    local rss_per_conn
    rss_per_conn=$(awk "BEGIN{ if (${TARGET_VUS} > 0) printf \"%.1f\", ${rss_peak}/${TARGET_VUS}; else print 0 }")

    {
        echo -e "label\tmode\trss_kb\trss_per_conn_kb\theap_committed_kb\tplatform_threads"
        echo -e "${label}\t${MODE}\t${rss_peak}\t${rss_per_conn}\t${heap_used}\t${nthreads}"
    } > "$out/summary.tsv"

    echo ">> $label  RSS=${rss_peak}KB  RSS/conn=${rss_per_conn}KB  heap=${heap_used}KB  threads=${nthreads}"
}

# ----------------------------- main ----------------------------------------
for s in "${SERVERS[@]}"; do
    IFS='|' read -r label main <<< "$s"
    run_one "$label" "$main"
done

echo ""
echo "==================== COMPARISON ===================="
echo "Results dir: $RESULTS_DIR/*-${MODE}-${STAMP}/"
echo
for f in "$RESULTS_DIR"/*-${MODE}-${STAMP}/summary.tsv; do
    [[ -f "$f" ]] || continue
    echo "--- $(dirname "$f") ---"
        column -t -s "$(printf '\t')" "$f" 2>/dev/null || cat "$f"
    echo
done

echo "Tips:"
echo "  - idle mode      -> compare rss_per_conn_kb and platform_threads (lower = better memory)."
echo "  - throughput mode-> compare echo_rtt_ms (p95/p99) in k6-summary.json and ws_msgs_received rate."

