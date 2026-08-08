import ws from 'k6/ws';
import { check } from 'k6';

// ─── Virtual Thread Flood Test ───────────────────────────────────────────────
// Goal: open many concurrent long-lived WebSocket connections so the server
// creates one virtual thread per connection.  While this test runs you can
// observe:
//
//   jvm_threads_virtual              ≈ number of connected VUs
//   jvm_threads_platform             ≈ baseline platform threads + sampler
//   jvm_threads_state{state="…"}     per-state breakdown of platform threads
//   websocket_concurrent_users       ≈ jvm_threads_virtual (should match)
//
// ──────────────────────────────────────────────────────────────────────────────

// ── How many VUs to maintain simultaneously ──────────────────────────────────
// k6 runs each VU as a separate JavaScript runtime inside a goroutine.  A
// single k6 process can realistically drive a few thousand VUs before it runs
// out of CPU / memory.  Pick a value that your test machine can handle:
//
//   Local laptop (8–16 GB RAM)   →  500 – 2 000
//   Dedicated test box (32 GB)   →  2 000 – 5 000
//   100 000                      →  split across ~20 k6 instances (see footer)
//
const TARGET_VUS = 1_000;

// ── How long each VU holds its connection open ───────────────────────────────
// The connection stays alive the whole time, so the server keeps the virtual
// thread mounted (or parked on i/o).  A longer duration gives you more time to
// inspect metrics in Grafana / Prometheus.
const HOLD_MINUTES = 5;

// ── Keep-alive ping interval (seconds) ───────────────────────────────────────
// Prevents intermediate proxies / load-balancers from dropping idle connections.
// Must be shorter than any infra idle timeout (typically 60–300 s).
const PING_INTERVAL_S = 15;

// ── WebSocket endpoint ───────────────────────────────────────────────────────
const WS_URL = 'ws://localhost:8189/echo';

// ── k6 scenario ──────────────────────────────────────────────────────────────
// constant-vus: all VUs start at second 0 and keep their connections open for
// the full duration. Every VU is concurrent — no ramp, no iteration loop.
export const options = {
    scenarios: {
        flood: {
            executor:  'constant-vus',
            vus:       TARGET_VUS,
            duration:  `${HOLD_MINUTES}m`,
        },
    },

    thresholds: {
        // Make sure every VU actually got a WebSocket session
        'ws_sessions':      [`count>=${TARGET_VUS}`],
        // At least one message was sent and received across all VUs
        'ws_msgs_sent':     ['count>0'],
        'ws_msgs_received': ['count>0'],
    },
};

// ── Per-VU script ────────────────────────────────────────────────────────────
export default function () {
    // Each VU opens one WebSocket and holds it until the test ends.
    // k6 will retry the connect call if the server is temporarily overloaded
    // (TCP backlog full), so the VU doesn't exit early.

    const res = ws.connect(WS_URL, { tags: { test_type: 'virtual-thread-flood' } }, function (socket) {

        // --- open: handshake completed, virtual thread is now alive on server
        socket.on('open', function () {
            // Send one payload so there is at least one ws_msgs_sent data point
            socket.send(JSON.stringify({
                type:      'hello',
                vu:        __VU,
                timestamp: Date.now(),
            }));

            // Periodic ping to keep the connection from appearing idle
            socket.setInterval(function () {
                socket.ping();
            }, PING_INTERVAL_S * 1000);
        });

        // --- message: validate the echo response
        socket.on('message', function (data) {
            try {
                const msg = JSON.parse(data);
                check(msg, {
                    'valid echo': (m) =>
                        m.type === 'hello' && m.timestamp !== undefined,
                });
            } catch (_) {
                // ignore non-JSON frames (pong, close, etc.)
            }
        });

        // --- error: log anything unexpected
        socket.on('error', function (e) {
            const err = e.error();
            // "close sent" is normal during ramp-down, ignore it
            if (err !== 'websocket: close sent') {
                console.error(`[VU ${__VU}] ${err}`);
            }
        });

        // --- close
        socket.on('close', function () {
            console.log(`[VU ${__VU}] disconnected`);
        });

        // --- safety timeout: slightly longer than the scenario duration
        socket.setTimeout(function () {
            socket.close();
        }, (HOLD_MINUTES * 60 + 30) * 1000);
    });

    // --- verify the HTTP upgrade was successful
    check(res, {
        'handshake 101': (r) => r && r.status === 101,
    });
}

// ── OS tuning for very high connection counts ────────────────────────────────
// When scaling beyond ~1 000 concurrent connections on a single box:
//
//   # Linux
//   sudo sysctl -w net.core.somaxconn=65535
//   sudo sysctl -w net.ipv4.tcp_tw_reuse=1
//   sudo sysctl -w net.ipv4.ip_local_port_range="1024 65535"
//   ulimit -n 200000
//
//   # macOS
//   sudo sysctl -w kern.maxfiles=200000
//   sudo sysctl -w kern.maxfilesperproc=200000
//   ulimit -n 200000
//
// ── Scaling to 100k VUs ──────────────────────────────────────────────────────
// Split across multiple k6 processes using execution segments:
//
//   # 20 instances × 5 000 VUs = 100 000 concurrent connections
//   for i in $(seq 0 19); do
//       k6 run --execution-segment "${i}/20:$((i+1))/20" \
//           -e TARGET_VUS=5000 \
//           k6-virtual-thread-flood.ts &
//   done
//
// Or use the k6 Operator on Kubernetes to fan out across many pods.
// ──────────────────────────────────────────────────────────────────────────────
