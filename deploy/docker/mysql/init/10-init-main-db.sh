#!/usr/bin/env bash
set -e

main_db="${MYSQL_DATABASE:-campus_life_demo}"
schema_file="/docker-entrypoint-initdb.d/sql/backend-docs/db-schema.sql"
migration_dir="/docker-entrypoint-initdb.d/sql/backend-migrations"

run_main_sql() {
    local file="$1"
    if [ ! -f "$file" ]; then
        echo "[init-main-db] missing SQL file: $file" >&2
        exit 1
    fi
    echo "[init-main-db] importing $file"
    docker_process_sql --database="$main_db" < "$file"
}

echo "[init-main-db] initializing database: $main_db"

run_main_sql "$schema_file"
# db-schema.sql already contains shopping_cart and idx_cart_user_update_time.
# Running migrate-shopping-cart-v1.sql after the current schema would duplicate the index.
run_main_sql "$migration_dir/migrate-payment-domain-v1.sql"
run_main_sql "$migration_dir/migrate-payment-reconciliation-v1.sql"
run_main_sql "$migration_dir/migrate-payment-refund-review-v1.sql"
run_main_sql "$migration_dir/migrate-payment-wallet-backfill-v1.sql"
run_main_sql "$migration_dir/migrate-system-notification.sql"
run_main_sql "$migration_dir/migrate-merchant-location-v1.sql"
run_main_sql "$migration_dir/migrate-voucher-wallet-v2.sql"
run_main_sql "$migration_dir/migrate-voucher-order-v3.sql"
run_main_sql "$migration_dir/migrate-voucher-order-idempotency-v4.sql"
run_main_sql "$migration_dir/migrate-voucher-order-use-deadline-v5.sql"
run_main_sql "$migration_dir/migrate-voucher-order-order-no-unique.sql"
run_main_sql "$migration_dir/migrate-event-reliability-v1.sql"
run_main_sql "$migration_dir/migrate-performance-indexes-v1.sql"

echo "[init-main-db] finished"
