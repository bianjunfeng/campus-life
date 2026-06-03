#!/usr/bin/env bash
set -e

ai_db="${AI_MYSQL_DATABASE:-campus_life_ai_demo}"
ai_user="${AI_MYSQL_USER:-campus_life_ai_user}"
ai_password="${AI_MYSQL_PASSWORD:-CHANGE_ME_ai_mysql_password}"
ai_password_sql="${ai_password//\'/\'\'}"
migration_dir="/docker-entrypoint-initdb.d/sql/ai-migrations"

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

for file in "$migration_dir"/V*.sql; do
    if [ ! -f "$file" ]; then
        echo "[init-ai-db] missing AI migration files in $migration_dir" >&2
        exit 1
    fi
    echo "[init-ai-db] importing $file"
    docker_process_sql --database="$ai_db" < "$file"
done

echo "[init-ai-db] finished"

