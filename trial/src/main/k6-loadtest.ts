import ws from 'k6/ws';
import { check, sleep } from 'k6';

// --- Load test configuration ---
export const options = {
    vus: 100,
    stages: [
        { duration: '10s', target: 10 },
        { duration: '30s', target: 10 },
        { duration: '10s', target: 0  },
    ],
    thresholds: {
        'ws_session_duration':  ['p(95)<5000'],  // 95% of sessions under 5s
        'ws_sessions':          ['count>0'],      // at least 1 session opened
        'ws_msgs_received':     ['count>0'],      // at least 1 message received
        'ws_msgs_sent':         ['count>0'],      // at least 1 message sent
    },
};

export default function () {
    const url    = 'ws://localhost:8189/echo';
    const params = {
        tags:    { test_type: 'websocket' },
        headers: { 'X-Custom-Header': 'k6-load-test' },
    };

    const res = ws.connect(url, params, function (socket) {

        // 1. On open: send a message + start periodic ping
        socket.on('open', function () {
            console.log(`[VU ${__VU}] Connected`);

            // Send initial payload
            socket.send(JSON.stringify({ type: 'hello', timestamp: Date.now() }));

            // Ping every 2 seconds to keep connection alive
            socket.setInterval(function () {
                socket.ping();
            }, 2000);
        });

        // 2. On message: parse and validate response
        socket.on('message', function (data) {
            console.log(`[VU ${__VU}] Message received: ${data}`);

            try {
                const msg = JSON.parse(data);
                check(msg, {
                    'response has timestamp': (m) => m.timestamp !== undefined,
                    'response type is hello': (m) => m.type === 'hello',
                });
            } catch (e) {
                console.warn(`[VU ${__VU}] Non-JSON message: ${data}`);
            }
        });

        // 3. Ping/Pong handlers
        socket.on('ping', () => console.log(`[VU ${__VU}] PING received`));
        socket.on('pong', () => console.log(`[VU ${__VU}] PONG received`));

        // 4. Error handler
        socket.on('error', function (e) {
            if (e.error() !== 'websocket: close sent') {
                console.error(`[VU ${__VU}] Unexpected error: ${e.error()}`);
            }
        });

        // 5. Close handler
        socket.on('close', function () {
            console.log(`[VU ${__VU}] Disconnected`);
        });

        // 6. Auto-close after 5 seconds per VU iteration
        socket.setTimeout(function () {
            console.log(`[VU ${__VU}] Timeout reached, closing socket`);
            socket.close();
        }, 5000);
    });

    // Check that upgrade handshake was successful (HTTP 101)
    check(res, {
        'WebSocket handshake status 101': (r) => r && r.status === 101,
    });

    sleep(1);
}