#!/usr/bin/env bash
#
# Reproduces the WebSocket handshake fragmentation bug.
#
# The server parses the HTTP upgrade request from a SINGLE read(). If the
# request arrives split across TCP segments (as a proxy such as `tailscale
# serve` can do), the parse sees an incomplete request -> "null null null" ->
# handshake rejected -> connection reset. A one-shot handshake works.
#
#   ./scripts/repro-fragmented-handshake.sh
#
# Env:
#   PORT         port to start/use        (default 18445)
#   SERVER_TYPE  thread-per-connection|nio (default thread-per-connection)
#   TARGET       host:port of an already-running server (skips starting one)
#
set -uo pipefail
set +m

PORT="${PORT:-18445}"
SERVER_TYPE="${SERVER_TYPE:-thread-per-connection}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="$ROOT/free-draw/build/libs/free-note-service-0.0.1-SNAPSHOT-all.jar"
CONF="$(mktemp -d)"
PID=""

cleanup() {
  if [ -n "$PID" ]; then
    kill "$PID" 2>/dev/null
    for _ in 1 2 3 4; do kill -0 "$PID" 2>/dev/null || break; sleep 0.5; done
    kill -9 "$PID" 2>/dev/null
  fi
  rm -rf "$CONF"
}
trap cleanup EXIT

if [ -n "${TARGET:-}" ]; then
  HOST="${TARGET%%:*}"
  PORT="${TARGET##*:}"
  echo "Testing already-running server at $TARGET"
else
  HOST="127.0.0.1"

  if [ -n "${JAVA_BIN:-}" ]; then
    JAVA="$JAVA_BIN"
  elif JAVA_HOME_21="$(/usr/libexec/java_home -v 21 2>/dev/null)"; then
    JAVA="$JAVA_HOME_21/bin/java"
  else
    JAVA="java"
  fi

  if [ ! -f "$JAR" ]; then
    echo "==> Building shadowJar..."
    (cd "$ROOT" && ./gradlew :free-draw:shadowJar -q) || exit 1
  fi

  cat > "$CONF/application.properties" <<EOF
freenote.profiles.active=default
freenote.active.port=$PORT
freenote.connection.type=$SERVER_TYPE
freenote.server.type=$SERVER_TYPE
server.ssl.enabled=false
allowed.origins=https://free-note-ui.vercel.app
prometheus.port=19464
EOF

  echo "==> Starting FreeNoteApplication (server.type=$SERVER_TYPE) on $PORT ..."
  "$JAVA" -cp "$CONF:$JAR" com.freedraw.legacy.FreeNoteApplication > "$CONF/server.log" 2>&1 </dev/null &
  PID=$!
  disown 2>/dev/null || true

  for _ in $(seq 1 30); do
    (exec 3<>"/dev/tcp/$HOST/$PORT") 2>/dev/null && break
    sleep 1
  done
fi

python3 - "$HOST" "$PORT" <<'PY'
import socket, sys, time

host, port = sys.argv[1], int(sys.argv[2])
key = "dGhlIHNhbXBsZSBub25jZQ=="
origin = "https://free-note-ui.vercel.app"
request = (
    f"GET /freeNote HTTP/1.1\r\n"
    f"Host: {host}:{port}\r\n"
    f"Upgrade: websocket\r\n"
    f"Connection: Upgrade\r\n"
    f"Sec-WebSocket-Key: {key}\r\n"
    f"Origin: {origin}\r\n"
    f"Sec-WebSocket-Version: 13\r\n"
    f"\r\n"
).encode()

def attempt(name, chunks, delay):
    s = socket.create_connection((host, port), timeout=6)
    try:
        for chunk in chunks:
            s.sendall(chunk)
            if delay:
                time.sleep(delay)
        s.settimeout(4)
        data = s.recv(4096)
        status = data.split(b"\r\n", 1)[0]
        ok = b"101" in status
        print(f"[{'PASS' if ok else 'FAIL'}] {name}: {status.decode(errors='replace')!r}")
        return ok
    except Exception as e:
        print(f"[FAIL] {name}: {type(e).__name__}: {e}")
        return False
    finally:
        s.close()

print("\n=== 1) single-packet handshake ===")
single_ok = attempt("one write()", [request], 0)

telemetry, header_end = request.split(b"\r\n", 1)
print("\n=== 2) fragmented handshake (request line, 1s pause, then headers) ===")
frag_ok = attempt("two writes()", [telemetry + b"\r\n", header_end], 1.0)

print()
if single_ok and not frag_ok:
    print(">>> BUG REPRODUCED: one-shot handshake works, fragmented handshake is rejected.")
    sys.exit(1)
elif single_ok and frag_ok:
    print(">>> OK: both handshakes accepted.")
    sys.exit(0)
else:
    print(">>> UNEXPECTED: even the single-packet handshake failed (server/config issue?).")
    sys.exit(2)
PY
status=$?

if [ -n "$PID" ] && [ -f "$CONF/server.log" ]; then
  echo
  echo "=== server log (handshake lines) ==="
  grep -aE "Performing handshake|not approved|Handshake failed" "$CONF/server.log" | tail -5
fi

exit "$status"
