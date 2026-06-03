#!/usr/bin/env bash
set -e

ai_db="${AI_MYSQL_DATABASE:-campus_life_ai_demo}"
ai_user="${AI_MYSQL_USER:-campus_life_ai_user}"
ai_password="${AI_MYSQL_PASSWORD:-CHANGE_ME_ai_mysql_password}"
ai_password_sql="${ai_password//\'/\'\'}"
init_mode="${AI_DB_INIT_MODE:-schema}"
dump_dir="/docker-entrypoint-initdb.d/dumps/campus_life_ai"

case "$init_mode" in
  schema)
    dump_file="$dump_dir/schema.sql"
    ;;
  full)
    dump_file="$dump_dir/full.sql"
    ;;
  *)
    echo "[init-ai-db] invalid AI_DB_INIT_MODE: $init_mode (use schema or full)" >&2
    exit 1
    ;;
esac

if [ ! -f "$dump_file" ]; then
  echo "[init-ai-db] missing dump: $dump_file" >&2
  exit 1
fi

echo "[init-ai-db] creating database and user: $ai_db / $ai_user"

docker_process_sql --database=mysql <<-EOSQL
CREATE DATABASE IF NOT EXISTS \`${ai_db}\`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS '${ai_user}'@'%' IDENTIFIED BY '${ai_password_sql}';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP
    ON \`${ai_db}\`.* TO '${ai_user}'@'%';

FLUSH PRIVILEGES;
EOSQL

echo "[init-ai-db] importing $(basename "$dump_file") into $ai_db (AI_DB_INIT_MODE=$init_mode)"
docker_process_sql --database="$ai_db" < "$dump_file"
echo "[init-ai-db] finished"
