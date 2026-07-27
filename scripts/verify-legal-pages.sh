#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_DIR="${1:-${ROOT_DIR}/deploy-lighthouse}"
BASE_URL="${BASE_URL:-https://www.art1.cn}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing command: $1" >&2
    exit 127
  fi
}

print_ok() {
  printf '[OK] %s\n' "$1"
}

print_fail() {
  printf '[FAIL] %s\n' "$1" >&2
  exit 1
}

assert_file() {
  local path="$1"
  if [[ -f "${path}" ]]; then
    print_ok "found ${path}"
  else
    print_fail "missing ${path}"
  fi
}

assert_contains() {
  local path="$1"
  local pattern="$2"
  local label="$3"
  if grep -Fq "${pattern}" "${path}"; then
    print_ok "${label}"
  else
    echo "Checked file: ${path}" >&2
    sed -n '1,120p' "${path}" >&2
    print_fail "${label}"
  fi
}

check_remote() {
  local url="$1"
  local label="$2"
  local header_file="$3"
  local body_file="$4"

  local status
  status="$(curl -sS -L -o "${body_file}" -D "${header_file}" -w '%{http_code}' "${url}")" || print_fail "${label} unreachable"

  if [[ "${status}" != "200" ]]; then
    echo "Response headers for ${url}:" >&2
    sed -n '1,80p' "${header_file}" >&2
    echo "Response body for ${url}:" >&2
    sed -n '1,80p' "${body_file}" >&2
    print_fail "${label} returned HTTP ${status}"
  fi

  if grep -Eq '用户协议|隐私政策|/#/pages/user/agreement\?type=' "${body_file}"; then
    print_ok "${label}"
  else
    echo "Response body for ${url}:" >&2
    sed -n '1,120p' "${body_file}" >&2
    print_fail "${label} content check"
  fi
}

require_cmd curl
require_cmd grep
require_cmd sed

tmp_dir="$(mktemp -d)"
trap 'rm -rf "${tmp_dir}"' EXIT

agreement_file="${DEPLOY_DIR}/frontend-h5/agreement/index.html"
privacy_file="${DEPLOY_DIR}/frontend-h5/privacy/index.html"
nginx_conf="${DEPLOY_DIR}/config/nginx-h5.conf"

echo "Checking local deploy artifacts in ${DEPLOY_DIR}"
assert_file "${agreement_file}"
assert_file "${privacy_file}"
assert_file "${nginx_conf}"

assert_contains "${agreement_file}" "/#/pages/user/agreement?type=terms" "agreement redirect target"
assert_contains "${privacy_file}" "/#/pages/user/agreement?type=privacy" "privacy redirect target"
assert_contains "${nginx_conf}" "location = /agreement" "nginx agreement route"
assert_contains "${nginx_conf}" "location = /privacy" "nginx privacy route"

echo
echo "Checking remote pages at ${BASE_URL}"
check_remote "${BASE_URL}/agreement" "remote agreement page" "${tmp_dir}/agreement.headers" "${tmp_dir}/agreement.body"
check_remote "${BASE_URL}/privacy" "remote privacy page" "${tmp_dir}/privacy.headers" "${tmp_dir}/privacy.body"

echo
echo "Legal pages verification passed."
