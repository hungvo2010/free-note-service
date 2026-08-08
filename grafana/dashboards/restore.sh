#!/bin/bash
# Restore the Network I/O Model Comparison dashboard to Grafana.
# Usage: GF_TOKEN=<token> ./grafana/dashboards/restore.sh [grafana-url]
#
# Get your token from: Grafana → Administration → Service Accounts → Add token

set -euo pipefail

GRAFANA_URL="${1:-http://home-laptop-server:3000}"
DASHBOARD_FILE="$(dirname "$0")/network-io-model-comparison.json"

if [ -z "${GF_TOKEN:-}" ]; then
    echo "GF_TOKEN is not set. Export it or pass it inline:"
    echo "  GF_TOKEN=glsa_... ./grafana/dashboards/restore.sh"
    exit 1
fi

curl -sS -X POST "${GRAFANA_URL}/api/dashboards/db" \
    -H "Authorization: Bearer ${GF_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{\"dashboard\": $(cat "$DASHBOARD_FILE"), \"overwrite\": true}" \
    | jq .
