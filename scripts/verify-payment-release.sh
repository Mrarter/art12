#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${1:-${ROOT_DIR}/deploy/docker/.env}"
BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-admin123}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing command: $1" >&2
    exit 127
  fi
}

require_cmd curl
require_cmd sed

"${ROOT_DIR}/scripts/check-payment-config.sh" "${ENV_FILE}"

tmp_dir="$(mktemp -d)"
trap 'rm -rf "${tmp_dir}"' EXIT

print_ok() {
  printf '[OK] %s\n' "$1"
}

print_fail() {
  printf '[FAIL] %s\n' "$1" >&2
  exit 1
}

extract_token() {
  sed -n 's/.*"token":"\([^"]*\)".*/\1/p' "$1" | head -n 1
}

assert_success_code() {
  local file="$1"
  local label="$2"
  if rg -q '"code"[[:space:]]*:[[:space:]]*200' "$file"; then
    print_ok "${label}"
  else
    echo "Response for ${label}:" >&2
    sed -n '1,120p' "$file" >&2
    print_fail "${label}"
  fi
}

echo "Checking gateway health at ${BASE_URL}"
health_file="${tmp_dir}/health.json"
curl -fsS "${BASE_URL}/health" > "${health_file}" || print_fail "gateway health endpoint unreachable"
print_ok "gateway health endpoint reachable"

echo
echo "Logging into admin API"
login_file="${tmp_dir}/login.json"
curl -fsS \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASS}\"}" \
  "${BASE_URL}/api/admin/login" > "${login_file}" || print_fail "admin login request failed"
assert_success_code "${login_file}" "admin login succeeded"

token="$(extract_token "${login_file}")"
if [[ -z "${token}" ]]; then
  sed -n '1,120p' "${login_file}" >&2
  print_fail "admin token missing in login response"
fi
print_ok "admin token received"

echo
echo "Checking payment admin endpoints"

payment_file="${tmp_dir}/payment-list.json"
curl -fsS \
  -H "Authorization: Bearer ${token}" \
  "${BASE_URL}/admin/order/payment/list?page=1&size=5" > "${payment_file}" || print_fail "payment list request failed"
assert_success_code "${payment_file}" "payment list endpoint"

notify_file="${tmp_dir}/payment-notify.json"
curl -fsS \
  -H "Authorization: Bearer ${token}" \
  "${BASE_URL}/admin/order/payment/notify-logs?page=1&size=5" > "${notify_file}" || print_fail "payment notify log request failed"
assert_success_code "${notify_file}" "payment notify logs endpoint"

refund_file="${tmp_dir}/aftersale-list.json"
curl -fsS \
  -H "Authorization: Bearer ${token}" \
  "${BASE_URL}/admin/order/aftersale/list?page=1&size=5" > "${refund_file}" || print_fail "aftersale list request failed"
assert_success_code "${refund_file}" "aftersale list endpoint"

echo
echo "Payment release verification passed."
