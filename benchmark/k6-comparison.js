import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

// ---------------------------------------------------------------------------
// k6 load generator for the NIO-vs-virtual-thread comparison.
//
// Two modes (selected via --env MODE):
//   idle        : open TARGET_VUS WebSocket sessions and HOLD them (with pings).
//                 Use this to measure STEADY-STATE MEMORY PER CONNECTION.
//                 This is the scenario where the two threading models differ
//                 the most (NIO: O(1) threads; vthread: 1 thread/conn).
//
//   throughput  : each VU opens a session, fires MSGS_PER_VU echo requests,
//                 measures round-trip latency, then closes. Use this to measure
//                 ECHO LATENCY (p50/p95/p99) and MESSAGES/SEC.
//
// All knobs are overridable with --env so the same script drives every run.
// ---------------------------------------------------------------------------

const TARGET_VUS  = parseInt(__ENV.TARGET_VUS || '200', 10);
const HOLD_SEC    = parseInt(__ENV.HOLD_SEC || '60', 10);
const MSGS_PER_VU = parseInt(__ENV.MSGS_PER_VU || '100', 10);
const URL         = __ENV.URL || 'ws://localhost:8189/echo';
const MODE        = (__ENV.MODE || 'idle').toLowerCase(); // 'idle' | 'throughput'

const echoRtt = new Trend('echo_rtt_ms', true); // echo round-trip, milliseconds
const echoed  = new Counter('echoed_messages');

const RAMP_SEC = '15s';

export const options = {
    scenarios: {
        ws_load: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: RAMP_SEC, target: TARGET_VUS },          // ramp up
                { duration: `${HOLD_SEC}s`, target: TARGET_VUS },    // hold
                { duration: RAMP_SEC, target: 0 },                   // ramp down
            ],
            gracefulRampDown: '5s',
            gracefulStop: '5s',
        },
    },
    thresholds: {
        'ws_sessions':      ['count>0'],   // at least one session opened
        'ws_msgs_received': ['count>0'],   // at least one message received
    },
};

export default function () {
    // Per-VU map of sent-message-id -> send timestamp (for RTT measurement).
    const sendTimes = {};

    const params = {
        tags: { test_type: 'websocket', server_mode: MODE },
        headers: { 'X-Benchmark': 'nio-vs-vthread' },
    };

    const res = ws.connect(URL, params, function (socket) {

        socket.on('open', function () {
            if (MODE === 'idle') {
                // Keep the connection alive with a ping every 3s, then close
                // a bit after the hold window so k6 controls teardown.
                socket.setInterval(() => socket.ping(), 3000);
                socket.setTimeout(() => socket.close(), (HOLD_SEC + 30) * 1000);
            } else {
                // Fire-and-collect: send MSGS_PER_VU echo requests.
                for (let i = 0; i < MSGS_PER_VU; i++) {
                    const id = `${__VU}-${__ITER}-${i}`;
                    sendTimes[id] = Date.now();
                    socket.send(id);
                }
                socket.setTimeout(() => socket.close(), 15000);
            }
        });

        socket.on('message', function (data) {
            // The /echo endpoint returns the same text frame we sent, so the
            // payload is our id. Record the round-trip time.
            const sentAt = sendTimes[data];
            if (sentAt !== undefined) {
                echoRtt.add(Date.now() - sentAt);
                echoed.add(1);
                delete sendTimes[data];
            }
        });

        socket.on('ping', () => { /* server-initiated ping */ });
        socket.on('pong', () => { /* our ping answered */ });

        socket.on('error', function (e) {
            const msg = e && e.error ? e.error() : String(e);
            if (msg !== 'websocket: close sent') {
                console.error(`[VU ${__VU}] error: ${msg}`);
            }
        });

        socket.on('close', () => { /* session ended */ });
    });

    // Verify the HTTP 101 Switching Protocols upgrade succeeded.
    check(res, {
        'handshake status 101': (r) => r && r.status === 101,
    });

    sleep(0.1);
}
