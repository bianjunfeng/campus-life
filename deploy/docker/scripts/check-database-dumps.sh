#!/usr/bin/env bash
set -euo pipefail

DB_ROOT="$(cd "$(dirname "$0")/../.." && pwd)/database"

required=(
  "$DB_ROOT/campus_life/schema.sql"
  "$DB_ROOT/campus_life/full.sql"
  "$DB_ROOT/campus_life_ai/schema.sql"
  "$DB_ROOT/campus_life_ai/full.sql"
  "$DB_ROOT/MANIFEST.json"
)

missing=0
for file in "${required[@]}"; do
  if [ ! -s "$file" ]; then
    echo "[check-database-dumps] missing or empty: $file" >&2
    missing=1
  fi
done

if [ "$missing" -ne 0 ]; then
  echo "[check-database-dumps] run: bash deploy/database/scripts/export-from-running.sh" >&2
  exit 1
fi

echo "[check-database-dumps] OK"
