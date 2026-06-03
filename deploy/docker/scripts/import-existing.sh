#!/usr/bin/env bash
# 对已有 mysql 卷手动导入 full 快照（会覆盖同表数据，请先备份）
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
COMPOSE_DIR="$(dirname "$SCRIPT_DIR")"
DB_ROOT="$(cd "${COMPOSE_DIR}/../database" && pwd)"

cd "$COMPOSE_DIR"

if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:?set MYSQL_ROOT_PASSWORD in .env}"
MYSQL_DATABASE="${MYSQL_DATABASE:-campus_life_demo}"
AI_MYSQL_DATABASE="${AI_MYSQL_DATABASE:-campus_life_ai_demo}"
MAIN_MODE="${1:-full}"
AI_MODE="${2:-full}"

import_dump() {
  local db="$1"
  local mode="$2"
  local subdir="$3"
  local file

  case "$mode" in
    schema) file="${DB_ROOT}/${subdir}/schema.sql" ;;
    full) file="${DB_ROOT}/${subdir}/full.sql" ;;
    *) echo "invalid mode: $mode (use schema or full)" >&2; exit 1 ;;
  esac

  if [ ! -f "$file" ]; then
    echo "[import-existing] missing: $file" >&2
    exit 1
  fi

  echo "[import-existing] importing $(basename "$file") -> $db"
  docker compose exec -T mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" "${db}" <"$file"
}

if ! docker compose ps --status running mysql 2>/dev/null | grep -q mysql; then
  echo "[import-existing] start mysql first: docker compose up -d mysql" >&2
  exit 1
fi

import_dump "$MYSQL_DATABASE" "$MAIN_MODE" "campus_life"
import_dump "$AI_MYSQL_DATABASE" "$AI_MODE" "campus_life_ai"

echo "[import-existing] done"
