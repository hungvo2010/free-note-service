#!/usr/bin/env bash
#
# Manage a Toxiproxy proxy + toxics for the benchmark via its HTTP API.
# No toxiproxy-cli dependency — just curl.
#
# Usage:
#   UPSTREAM=host.docker.internal:8189 ./benchmark/toxiproxy-control.sh create
#   LATENCY_MS=200 JITTER_MS=50 ./benchmark/toxiproxy-control.sh add
#   ./benchmark/toxiproxy-control.sh reset     # remove all toxics, keep proxy
#   ./benchmark/toxiproxy-control.sh status
#
set -euo pipefail

API="${TOXIPROXY_API:-http://localhost:8474}"
PROXY_NAME="${PROXY_NAME:-freenote}"
LISTEN="${LISTEN_PORT:-18189}"
UPSTREAM="${UPSTREAM:-host.docker.internal:8189}"

maybe_jq() { if command -v jq >/dev/null 2>&1; then jq -c .; else cat; fi; }

add_toxic() {
    local name="$1" type="$2" stream="$3" attr="$4"
    curl -fsS -X POST "$API/proxies/$PROXY_NAME/toxics" \
        -H 'Content-Type: application/json' \
        -d "{\"name\":\"$name\",\"type\":\"$type\",\"stream\":\"$stream\",\"toxicity\":1.0,\"attributes\":$attr}" \
        >/dev/null && echo "  + $name ($type/$stream)"
}

case "${1:-status}" in
    create)
        # idempotent: remove an existing proxy with the same name, then create.
        curl -fsS -X DELETE "$API/proxies/$PROXY_NAME" >/dev/null 2>&1 || true
        curl -fsS -X POST "$API/proxies" \
            -H 'Content-Type: application/json' \
            -d "{\"name\":\"$PROXY_NAME\",\"listen\":\"0.0.0.0:$LISTEN\",\"upstream\":\"$UPSTREAM\",\"enabled\":true}" \
            | maybe_jq
        echo ">> proxy '$PROXY_NAME' :$LISTEN -> $UPSTREAM"
        ;;
    reset)
        curl -fsS -X POST "$API/proxies/$PROXY_NAME/reset" >/dev/null && echo ">> toxics reset (proxy kept)"
        ;;
    add)
        echo "Applying toxics:"
        # Each toxic is added only when its env var is set.
        [[ -n "${LATENCY_MS:-}" ]]      && add_toxic latency   latency     downstream "{\"latency\":${LATENCY_MS},\"jitter\":${JITTER_MS:-0}}"
        [[ -n "${BANDWIDTH_KB:-}" ]]    && add_toxic bandwidth bandwidth   downstream "{\"rate\":$((BANDWIDTH_KB*1024*8))}"
        [[ -n "${SLOW_CLOSE_MS:-}" ]]   && add_toxic slowclose slow_close  downstream "{\"delay\":${SLOW_CLOSE_MS}}"
        [[ -n "${TIMEOUT_MS:-}" ]]      && add_toxic timeout   timeout     downstream "{\"timeout\":${TIMEOUT_MS}}"
        [[ -n "${LIMIT_DATA_KB:-}" ]]   && add_toxic limitdata limit_data  downstream "{\"bytes\":$((LIMIT_DATA_KB*1024))}"
        [[ -n "${SLICER:-}" ]]          && add_toxic slicer    slicer      downstream "{\"average_size\":${SLICER_SIZE:-1024},\"delay\":${SLICER_DELAY_MS:-10}}"
        echo "done"
        ;;
    status)
        curl -fsS "$API/proxies" | maybe_jq
        ;;
    *)
        echo "usage: $0 {create|reset|add|status}" >&2
        exit 1
        ;;
esac
