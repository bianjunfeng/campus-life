#!/usr/bin/env bash
set -e

main_db="${MYSQL_DATABASE:-campus_life_demo}"
init_mode="${DB_INIT_MODE:-schema}"
dump_dir="/docker-entrypoint-initdb.d/dumps/campus_life"

case "$init_mode" in
  schema)
    dump_file="$dump_dir/schema.sql"
    ;;
  full)
    dump_file="$dump_dir/full.sql"
    ;;
  *)
    echo "[init-main-db] invalid DB_INIT_MODE: $init_mode (use schema or full)" >&2
    exit 1
    ;;
esac

if [ ! -f "$dump_file" ]; then
  echo "[init-main-db] missing dump: $dump_file" >&2
  exit 1
fi

echo "[init-main-db] importing $(basename "$dump_file") into $main_db (DB_INIT_MODE=$init_mode)"
docker_process_sql --database="$main_db" < "$dump_file"
echo "[init-main-db] finished"
