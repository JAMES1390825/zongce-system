#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

PORT=8082

existing_pid="$(lsof -t -iTCP:${PORT} -sTCP:LISTEN 2>/dev/null | head -n 1 || true)"
if [[ -n "${existing_pid}" ]]; then
	echo "[0/2] Port ${PORT} is in use by PID ${existing_pid}, stopping it..."
	kill "${existing_pid}" || true
	sleep 1
fi

echo "[1/2] Building project..."
mvn -DskipTests package

echo "[2/2] Starting app on http://localhost:${PORT} ..."
java -jar target/zongce-system-0.0.1-SNAPSHOT.jar
