#!/usr/bin/env bash
set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-zongce}"
DB_USER="${DB_USER:-zongce}"
DB_PASSWORD="${DB_PASSWORD:-zongce123!}"
OUT_DIR="${OUT_DIR:-./backups}"

mkdir -p "${OUT_DIR}"
TS="$(date +%Y%m%d-%H%M%S)"
OUT_FILE="${OUT_DIR}/${DB_NAME}-${TS}.sql"

echo "[backup] exporting ${DB_NAME} -> ${OUT_FILE}"
mysqldump \
  --host="${DB_HOST}" \
  --port="${DB_PORT}" \
  --user="${DB_USER}" \
  --password="${DB_PASSWORD}" \
  --single-transaction \
  --quick \
  "${DB_NAME}" > "${OUT_FILE}"

echo "[backup] done"
