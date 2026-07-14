#!/usr/bin/env bash

for pidfile in /tmp/sagemaker-backend.pid /tmp/sagemaker-next.pid /tmp/sagemaker-proxy.pid; do
  if [ -f "$pidfile" ]; then
    pid=$(cat "$pidfile")
    if kill -0 "$pid" 2>/dev/null; then
      kill "$pid" 2>/dev/null || true
    fi
    rm -f "$pidfile"
  fi
done

# Kill anything still on ports 3000, 3001, 8080
for port in 3000 3001 8080; do
  pids=$(lsof -ti ":$port" 2>/dev/null || true)
  if [ -n "$pids" ]; then
    echo "Killing processes on port $port: $pids"
    echo "$pids" | xargs kill 2>/dev/null || true
  fi
done

echo "SageMaker dev environment stopped."
