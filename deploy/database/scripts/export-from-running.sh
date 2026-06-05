#!/usr/bin/env bash
# 从当前运行的 MySQL 导出 schema / full 两个版本到 deploy/database/
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DB_ROOT="$(dirname "$SCRIPT_DIR")"
REPO_ROOT="$(cd "${DB_ROOT}/.." && pwd)"

MYSQL_CONTAINER="${MYSQL_CONTAINER:-campus-life-mysql}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root123456}"
MAIN_DB="${MAIN_DB:-campus_life}"
AI_DB="${AI_DB:-campus_life_ai}"

dump_one() {
  local db="$1"
  local out_dir="$2"
  local tmp="/tmp/${db}"

  mkdir -p "$out_dir"

  echo "[export] $db -> schema.sql"
  docker exec "$MYSQL_CONTAINER" sh -c \
    "mysqldump -uroot -p'${MYSQL_ROOT_PASSWORD}' --no-data --routines --triggers --single-transaction --set-gtid-purged=OFF '${db}' > ${tmp}_schema.sql"
  docker cp "${MYSQL_CONTAINER}:${tmp}_schema.sql" "${out_dir}/schema.sql"

  echo "[export] $db -> full.sql"
  docker exec "$MYSQL_CONTAINER" sh -c \
    "mysqldump -uroot -p'${MYSQL_ROOT_PASSWORD}' --routines --triggers --single-transaction --set-gtid-purged=OFF '${db}' > ${tmp}_full.sql"
  docker cp "${MYSQL_CONTAINER}:${tmp}_full.sql" "${out_dir}/full.sql"
}

if ! docker ps --format '{{.Names}}' | grep -qx "$MYSQL_CONTAINER"; then
  echo "MySQL container not running: $MYSQL_CONTAINER" >&2
  exit 1
fi

dump_one "$MAIN_DB" "${DB_ROOT}/campus_life"
dump_one "$AI_DB" "${DB_ROOT}/campus_life_ai"

exported_at="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
main_tables="$(docker exec "$MYSQL_CONTAINER" mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" -N -e \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${MAIN_DB}'" 2>/dev/null)"
ai_tables="$(docker exec "$MYSQL_CONTAINER" mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" -N -e \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${AI_DB}'" 2>/dev/null)"

cat > "${DB_ROOT}/MANIFEST.json" <<EOF
{
  "exportedAt": "${exported_at}",
  "source": {
    "container": "${MYSQL_CONTAINER}",
    "mainDatabase": "${MAIN_DB}",
    "aiDatabase": "${AI_DB}"
  },
  "campus_life": {
    "tables": ${main_tables},
    "schemaFile": "campus_life/schema.sql",
    "fullFile": "campus_life/full.sql"
  },
  "campus_life_ai": {
    "tables": ${ai_tables},
    "schemaFile": "campus_life_ai/schema.sql",
    "fullFile": "campus_life_ai/full.sql"
  }
}
EOF

echo "[export] wrote ${DB_ROOT}/MANIFEST.json"
echo "[export] done"
