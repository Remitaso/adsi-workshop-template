#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Stop existing processes
bash "$SCRIPT_DIR/dev-sagemaker-stop.sh"

# Environment
export SAGEMAKER=1
export NEXT_PUBLIC_BASE_PATH="/codeeditor/default/absports/3000"

# JAVA_HOME (SageMaker mise)
if [ -d "/opt/mise/installs/java/corretto-21" ]; then
  export JAVA_HOME="/opt/mise/installs/java/corretto-21"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

# Frontend install (if needed)
if [ ! -d "$PROJECT_ROOT/frontend/node_modules" ]; then
  echo "Installing frontend dependencies..."
  cd "$PROJECT_ROOT/frontend" && npm install
fi

# Frontend build
echo "Building frontend..."
cd "$PROJECT_ROOT/frontend"
SAGEMAKER=1 NEXT_PUBLIC_BASE_PATH="/codeeditor/default/absports/3000" npx next build

# Start backend (H2 workshop mode)
echo "Starting backend (local profile, H2)..."
cd "$PROJECT_ROOT/backend"
./gradlew bootRun --args='--spring.profiles.active=local' &
BACKEND_PID=$!
echo "$BACKEND_PID" > /tmp/sagemaker-backend.pid

# Start Next.js on 3001
echo "Starting Next.js on port 3001..."
cd "$PROJECT_ROOT/frontend"
SAGEMAKER=1 NEXT_PUBLIC_BASE_PATH="/codeeditor/default/absports/3000" \
  npx next start -H 127.0.0.1 -p 3001 &
NEXT_PID=$!
echo "$NEXT_PID" > /tmp/sagemaker-next.pid

# Wait for Next.js to be ready
sleep 2

# Start proxy on 3000
echo "Starting SageMaker proxy on port 3000..."
node "$PROJECT_ROOT/frontend/scripts/sagemaker-proxy.mjs" &
PROXY_PID=$!
echo "$PROXY_PID" > /tmp/sagemaker-proxy.pid

echo ""
echo "=== SageMaker dev environment started ==="
echo "  Backend:  http://localhost:8080"
echo "  Next.js:  http://127.0.0.1:3001 (internal)"
echo "  Proxy:    http://0.0.0.0:3000 (browser entry)"
echo ""
echo "Open PORTS tab -> globe icon on 3000 -> replace 'ports' with 'absports' in URL"
echo ""

wait
