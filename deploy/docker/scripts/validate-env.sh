#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
COMPOSE_DIR="$(dirname "$SCRIPT_DIR")"
ENV_FILE="${COMPOSE_DIR}/.env"

fail() {
  echo "[validate-env] ERROR: $*" >&2
  exit 1
}

warn() {
  echo "[validate-env] WARN: $*"
}

if [ ! -f "$ENV_FILE" ]; then
  fail "missing ${ENV_FILE} — run: cp .env.example .env"
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

contains_change_me() {
  local value="$1"
  [[ "$value" == *"CHANGE_ME"* ]]
}

check_secret() {
  local name="$1"
  local value="${2:-}"
  if [ -z "$value" ]; then
    fail "${name} is empty"
  fi
  if contains_change_me "$value"; then
    fail "${name} still uses placeholder CHANGE_ME"
  fi
}

check_secret "JWT_SECRET" "${JWT_SECRET:-}"
if [ "${#JWT_SECRET}" -lt 32 ]; then
  fail "JWT_SECRET must be at least 32 characters"
fi

check_secret "MYSQL_ROOT_PASSWORD" "${MYSQL_ROOT_PASSWORD:-}"
check_secret "MYSQL_PASSWORD" "${MYSQL_PASSWORD:-}"
check_secret "REDIS_PASSWORD" "${REDIS_PASSWORD:-}"
check_secret "RABBITMQ_DEFAULT_PASS" "${RABBITMQ_DEFAULT_PASS:-}"
check_secret "AI_MYSQL_PASSWORD" "${AI_MYSQL_PASSWORD:-}"
check_secret "AI_API_KEY_ENCRYPTION_SECRET" "${AI_API_KEY_ENCRYPTION_SECRET:-}"

if [ "${ALIPAY_ENABLED:-false}" = "true" ]; then
  if [[ "${ALIPAY_NOTIFY_URL:-}" != https://* ]]; then
    fail "ALIPAY_ENABLED=true requires ALIPAY_NOTIFY_URL to use https://"
  fi
  if [[ "${ALIPAY_RETURN_URL:-}" != https://* ]]; then
    fail "ALIPAY_ENABLED=true requires ALIPAY_RETURN_URL to use https://"
  fi
fi

if [ "${AI_OPENAI_COMPATIBLE_ENABLED:-false}" = "true" ] && [ -z "${AI_OPENAI_COMPATIBLE_API_KEY:-}" ]; then
  warn "AI_OPENAI_COMPATIBLE_ENABLED=true but AI_OPENAI_COMPATIBLE_API_KEY is empty"
fi

is_full_profile=false
if [[ ",${COMPOSE_PROFILES:-}," == *",full,"* ]]; then
  is_full_profile=true
fi
if [ "${AI_KNOWLEDGE_VECTOR_ENABLED:-false}" = "true" ]; then
  is_full_profile=true
fi

if [ "$is_full_profile" = "true" ]; then
  embedding_key="${AI_KNOWLEDGE_EMBEDDING_API_KEY:-${AI_OPENAI_COMPATIBLE_API_KEY:-}}"
  if [ -z "$embedding_key" ]; then
    fail "L2 vector search requires AI_KNOWLEDGE_EMBEDDING_API_KEY or AI_OPENAI_COMPATIBLE_API_KEY"
  fi
fi

echo "[validate-env] OK"
