#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/frontend"

if ! command -v npm >/dev/null 2>&1; then
  echo "[ERROR] npm 未安装，请先安装 Node.js 18+" >&2
  exit 1
fi

if [ ! -d node_modules ]; then
  echo "[1/2] Installing frontend dependencies..."
  npm install
else
  echo "[1/2] Dependencies already installed."
fi

echo "[2/2] Starting frontend on http://localhost:5173 ..."
npm run dev
