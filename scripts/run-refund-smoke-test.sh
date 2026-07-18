#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-admin123}"

if [[ $# -lt 3 ]]; then
  cat >&2 <<'USAGE'
Usage:
  scripts/run-refund-smoke-test.sh <user_id> <order_id> <refund_amount> [refund_id]

Example:
  BASE_URL=https://art1.cn scripts/run-refund-smoke-test.sh 1001 2002 0.01

Behavior:
  1. User side applies for refund.
  2. Admin side logs in and lists aftersale records.
  3. If refund_id is provided, the script approves that record.
  4. The script checks payment list and payment notify logs.
USAGE
  exit 64
fi

USER_ID="$1"
ORDER_ID="$2"
REFUND_AMOUNT="$3"
REFUND_ID="${4:-}"
REASON="${REASON:-支付退款联调}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing command: $1" >&2
    exit 127
  fi
}

require_cmd curl
require_cmd sed
require_cmd rg

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
    sed -n '1,160p' "$file" >&2
    print_fail "${label}"
  fi
}

echo "Step 1: apply refund as user ${USER_ID} for order ${ORDER_ID}"
apply_file="${tmp_dir}/apply-refund.json"
curl -fsS \
  -H 'Content-Type: application/json' \
  -H "X-User-Id: ${USER_ID}" \
  -d "{\"amount\":${REFUND_AMOUNT},\"reason\":\"${REASON}\",\"type\":\"refund\"}" \
  "${BASE_URL}/api/order/orders/${ORDER_ID}/refund" > "${apply_file}" || print_fail "apply refund request failed"
assert_success_code "${apply_file}" "user refund apply"

echo
echo "Step 2: admin login"
login_file="${tmp_dir}/login.json"
curl -fsS \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASS}\"}" \
  "${BASE_URL}/api/admin/login" > "${login_file}" || print_fail "admin login failed"
assert_success_code "${login_file}" "admin login"

token="$(extract_token "${login_file}")"
if [[ -z "${token}" ]]; then
  sed -n '1,160p' "${login_file}" >&2
  print_fail "admin token missing"
fi
print_ok "admin token received"

echo
echo "Step 3: inspect aftersale queue"
aftersale_file="${tmp_dir}/aftersale.json"
curl -fsS \
  -H "Authorization: Bearer ${token}" \
  "${BASE_URL}/admin/order/aftersale/list?page=1&size=20" > "${aftersale_file}" || print_fail "aftersale list request failed"
assert_success_code "${aftersale_file}" "aftersale list"

if [[ -z "${REFUND_ID}" ]]; then
  echo "No refund_id provided. Review this response and rerun with the exact refund record id to approve it:" >&2
  sed -n '1,160p' "${aftersale_file}" >&2
else
  echo
  echo "Step 4: approve aftersale record ${REFUND_ID}"
  approve_file="${tmp_dir}/approve.json"
  curl -fsS \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer ${token}" \
    -d "{\"id\":${REFUND_ID},\"status\":1,\"remark\":\"${REASON}\"}" \
    "${BASE_URL}/admin/order/aftersale/handle" > "${approve_file}" || print_fail "approve refund request failed"
  assert_success_code "${approve_file}" "approve refund"
fi

echo
echo "Step 5: inspect payment artifacts"
payment_file="${tmp_dir}/payment-list.json"
curl -fsS \
  -H "Authorization: Bearer ${token}" \
  "${BASE_URL}/admin/order/payment/list?page=1&size=20&keyword=${ORDER_ID}" > "${payment_file}" || print_fail "payment list request failed"
assert_success_code "${payment_file}" "payment list"

notify_file="${tmp_dir}/notify-logs.json"
curl -fsS \
  -H "Authorization: Bearer ${token}" \
  "${BASE_URL}/admin/order/payment/notify-logs?page=1&size=20&keyword=${ORDER_ID}" > "${notify_file}" || print_fail "notify logs request failed"
assert_success_code "${notify_file}" "payment notify logs"

echo
echo "Refund smoke test finished."
echo "Artifacts:"
echo "  apply:    ${apply_file}"
echo "  aftersale:${aftersale_file}"
echo "  payment:  ${payment_file}"
echo "  notify:   ${notify_file}"
