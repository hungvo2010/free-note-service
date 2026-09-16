#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

docker compose -f docker-compose.prod.yml up -d --build

# Wait for the plain-HTTP backend on 127.0.0.1:8189
for _ in $(seq 1 60); do
  (exec 3<>/dev/tcp/127.0.0.1/8189) 2>/dev/null && break
  sleep 2
done

# tailscale serve terminates TLS; backend MUST be plain http, no path
sudo tailscale serve reset
sudo tailscale serve --bg --https=443 http://127.0.0.1:8189

tailscale serve status || true
