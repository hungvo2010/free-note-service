import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';

// ---------------------------------------------------------------------------
// Application-level chaos: simulates real CLIENT behaviour.
// Pair with Toxiproxy (see run-chaos.sh) for NETWORK-level shaping.
//
// Scenarios (--env SCENARIO):
//   slow   : users that send a message every 1-10s (low engagement / typing)
//   stall  : a few quick messages, then a long silence (pings only), then resume
//   burst  : long idle, then a rapid burst of 50 messages back-to-back
//   churn  : rapid connect -> 1 message -> disconnect, looped (connection churn)
//   mixed  : a blend of the above distributed across VUs
//
// Knobs: --env SCENARIO, TARGET_VUS, HOLD_SEC, URL
// ---------------------------------------------------------------------------

const SCENARIO   = (__ENV.SCENARIO || 'slow').toLowerCase();
const TARGET_VUS = parseInt(__ENV.TARGET_VUS || '100', 10);
const HOLD_SEC   = parseInt(__ENV.HOLD_SEC || '60', 10);
const URL        = __ENV.URL || 'ws://localhost:18189/echo';

const rtt     = new Trend('chaos_rtt_ms', true);   // echo round-trip (ms)
const sent    = new Counter('chaos_msgs_sent');
const recvd   = new Counter('chaos_msgs_received');
const dropped = new Counter('chaos_disconnects');
const hsFail  = new Rate('chaos_handshake_failed'); // fraction of failed handshakes

export const options = {
    scenarios: {
        chaos: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: TARGET_VUS },
                { duration: `${HOLD_SEC}s`, target: TARGET_VUS },
                { duration: '10s', target: 0 },
            ],
            gracefulRampDown: '5s',
            gracefulStop: '5s',
        },
    },
};

const rnd = (min, max) => Math.random() * (max - min) + min;

export default function () {
    // Pick this VU's scenario. For 'mixed', spread scenarios across VUs so the
    // population is heterogeneous (some slow, some bursty, some churning).
    let s = SCENARIO;
    if (s === 'mixed') {
        s = ['slow', 'stall', 'burst', 'churn'][__VU % 4];
    }

    if (s === 'churn') {
        runChurn();
        return;
    }

    // slow / stall / burst : one long-lived session per VU. Block this VU for
    // the hold window so it does not open a second session.
    const pend = {}; // messageId -> send timestamp (for RTT)

    const res = ws.connect(URL, { tags: { scenario: s } }, function (socket) {

        socket.on('open', function () {
            socket.setInterval(() => socket.ping(), 3000); // keep-alive

            if (s === 'slow') {
                const slowLoop = () => {
                    const id = `${__VU}-${Date.now()}`;
                    pend[id] = Date.now();
                    socket.send(id);
                    sent.add(1);
                    socket.setTimeout(slowLoop, rnd(1000, 10000));
                };
                socket.setTimeout(slowLoop, rnd(500, 2000));

            } else if (s === 'stall') {
                for (let i = 0; i < 3; i++) {
                    const id = `${__VU}-init-${i}`;
                    pend[id] = Date.now();
                    socket.send(id);
                    sent.add(1);
                }
                // long silence (pings keep the socket open), then resume
                socket.setTimeout(() => {
                    const id = `${__VU}-resume`;
                    pend[id] = Date.now();
                    socket.send(id);
                    sent.add(1);
                }, (HOLD_SEC / 2) * 1000);

            } else if (s === 'burst') {
                socket.setTimeout(() => {
                    for (let i = 0; i < 50; i++) {
                        const id = `${__VU}-burst-${i}`;
                        pend[id] = Date.now();
                        socket.send(id);
                        sent.add(1);
                    }
                }, rnd(5000, HOLD_SEC * 1000 * 0.4));
            }

            socket.setTimeout(() => socket.close(), (HOLD_SEC + 20) * 1000);
        });

        socket.on('message', function (data) {
            const t = pend[data];
            if (t !== undefined) {
                rtt.add(Date.now() - t);
                recvd.add(1);
                delete pend[data];
            }
        });

        socket.on('error', function (e) {
            const m = e && e.error ? e.error() : String(e);
            if (m !== 'websocket: close sent') console.error(`[VU ${__VU}] ${m}`);
        });

        socket.on('close', () => { dropped.add(1); });
    });

    check(res, { 'handshake 101': (r) => r && r.status === 101 });
    hsFail.add(res && res.status === 101 ? 0 : 1);

    sleep(HOLD_SEC); // hold this VU so it keeps exactly one session open
}

function runChurn() {
    // Each iteration = one short-lived connection. k6 repeats default() for the
    // scenario duration, so this produces sustained connect/disconnect churn.
    const res = ws.connect(URL, { tags: { scenario: 'churn' } }, function (socket) {
        socket.on('open', function () {
            socket.send(`churn-${__VU}-${Date.now()}`);
            sent.add(1);
            socket.setTimeout(() => socket.close(), 200);
        });
        socket.on('message', () => { recvd.add(1); });
        socket.on('close', () => { dropped.add(1); });
    });
    check(res, { 'handshake 101': (r) => r && r.status === 101 });
    hsFail.add(res && res.status === 101 ? 0 : 1);
    sleep(rnd(0.05, 0.3));
}
