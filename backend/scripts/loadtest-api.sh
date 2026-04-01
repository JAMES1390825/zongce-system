#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8082}"
USERNAME="${USERNAME:-admin}"
INITIAL_PASSWORD="${INITIAL_PASSWORD:-123456}"
PASSWORD="${PASSWORD:-${INITIAL_PASSWORD}}"
CONCURRENCY="${CONCURRENCY:-20}"
REQUESTS="${REQUESTS:-200}"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT
COOKIE_FILE="${TMP_DIR}/cookie.txt"

echo "[loadtest] login ${USERNAME}"
LOGIN_HTTP_CODE="$(curl -s -o "${TMP_DIR}/login.json" -w "%{http_code}" \
  -c "${COOKIE_FILE}" \
  -H "Content-Type: application/json" \
  -X POST "${BASE_URL}/api/auth/login" \
  -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}")"

if [[ "${LOGIN_HTTP_CODE}" != "200" ]]; then
  echo "[loadtest] login failed: HTTP ${LOGIN_HTTP_CODE}"
  cat "${TMP_DIR}/login.json"
  exit 1
fi

echo "[loadtest] start ${REQUESTS} requests with concurrency ${CONCURRENCY}"
seq "${REQUESTS}" | xargs -I{} -P "${CONCURRENCY}" bash -c '
  code="$(curl -s -o /dev/null -w "%{http_code}" -b "'"${COOKIE_FILE}"'" "'"${BASE_URL}"'/api/auth/me")"
  [[ "$code" == "200" ]] || echo "$code"
'

echo "[loadtest] done"
