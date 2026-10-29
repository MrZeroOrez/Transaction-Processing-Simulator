#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
(cd frontend && npm ci)
(cd backend && ./mvnw spring-boot:run) &
BACKEND_PID=$!
trap 'kill "$BACKEND_PID" 2>/dev/null || true' EXIT INT TERM
(cd frontend && npm run dev)
