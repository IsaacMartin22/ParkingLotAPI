#!/usr/bin/env bash
# docker-entrypoint.sh
# Runs the bundled MCP server (private, 127.0.0.1 only) and the API service in one container.
# The MCP server starts first because the API fails startup if MCP is unreachable.
# If either process exits, the other is stopped and the container exits, so ECS/Docker restarts it.

set -uo pipefail

API_CMD=(java -javaagent:/app/newrelic/newrelic.jar -Dnewrelic.config.file=/app/newrelic/newrelic.yml)
# shellcheck disable=SC2206 # intentional word splitting of JVM options
API_CMD+=(${API_JAVA_OPTS:-} -jar /app/app.jar)

export MCP_SERVER_ADDRESS="${MCP_SERVER_ADDRESS:-127.0.0.1}"
export MCP_SERVER_PORT="${MCP_SERVER_PORT:-8081}"
export CHAT_MCP_BASE_URL="${CHAT_MCP_BASE_URL:-http://127.0.0.1:${MCP_SERVER_PORT}}"

mcp_pid=""
api_pid=""

stop_all() {
  [ -n "${api_pid}" ] && kill -TERM "${api_pid}" 2>/dev/null
  [ -n "${mcp_pid}" ] && kill -TERM "${mcp_pid}" 2>/dev/null
  return 0
}
trap stop_all TERM INT

echo "[entrypoint] Starting MCP server on ${MCP_SERVER_ADDRESS}:${MCP_SERVER_PORT}"
# shellcheck disable=SC2086 # intentional word splitting of JVM options
java ${MCP_JAVA_OPTS:--XX:MaxRAMPercentage=15 -XX:+UseSerialGC} -jar /app/mcp-server.jar &
mcp_pid=$!

mcp_ready=false
for _ in $(seq 1 "${MCP_STARTUP_TIMEOUT_SECONDS:-120}"); do
  if (exec 3<>"/dev/tcp/127.0.0.1/${MCP_SERVER_PORT}") 2>/dev/null; then
    mcp_ready=true
    break
  fi
  if ! kill -0 "${mcp_pid}" 2>/dev/null; then
    break
  fi
  sleep 1
done

if [ "${mcp_ready}" != "true" ]; then
  echo "[entrypoint] MCP server failed to start; exiting so the container is restarted." >&2
  stop_all
  wait
  exit 1
fi

echo "[entrypoint] MCP server is ready; starting API service"
"${API_CMD[@]}" &
api_pid=$!

wait -n "${mcp_pid}" "${api_pid}"
status=$?
echo "[entrypoint] A process exited (status ${status}); stopping the container." >&2
stop_all
wait
exit "${status}"
