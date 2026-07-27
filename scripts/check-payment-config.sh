#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_ENV="${1:-${ROOT_DIR}/deploy/docker/.env}"

if [[ ! -f "${DEPLOY_ENV}" ]]; then
  echo "Env file not found: ${DEPLOY_ENV}" >&2
  exit 66
fi

while IFS= read -r line || [[ -n "${line}" ]]; do
  [[ -z "${line}" ]] && continue
  [[ "${line}" =~ ^[[:space:]]*# ]] && continue
  [[ "${line}" != *=* ]] && continue

  key="${line%%=*}"
  value="${line#*=}"

  key="${key#"${key%%[![:space:]]*}"}"
  key="${key%"${key##*[![:space:]]}"}"

  export "${key}=${value}"
done < "${DEPLOY_ENV}"

TARGET_CERTS=(
  "${ROOT_DIR}/deploy/docker/certs/wxpay/apiclient_cert.p12"
  "${ROOT_DIR}/deploy-lighthouse/certs/wxpay/apiclient_cert.p12"
  "${ROOT_DIR}/deploy-lighthouse-light/certs/wxpay/apiclient_cert.p12"
)

COMPOSE_FILES=(
  "${ROOT_DIR}/deploy/docker/docker-compose.yml"
  "${ROOT_DIR}/deploy-lighthouse/docker-compose.yml"
  "${ROOT_DIR}/deploy-lighthouse-light/docker-compose.yml"
)

missing=0

print_ok() {
  printf '[OK] %s\n' "$1"
}

print_warn() {
  printf '[WARN] %s\n' "$1"
}

print_fail() {
  printf '[FAIL] %s\n' "$1"
  missing=1
}

echo "Checking env file: ${DEPLOY_ENV}"
echo

for compose_file in "${COMPOSE_FILES[@]}"; do
  if rg -q "WXPAY_KEY_PATH: /opt/shiyiju/certs/wxpay/apiclient_cert.p12" "${compose_file}" \
    && rg -q "\\./certs/wxpay:/opt/shiyiju/certs/wxpay:ro" "${compose_file}" \
    && rg -q "ALIPAY_ENABLED:" "${compose_file}"; then
    print_ok "compose wiring looks present: ${compose_file}"
  else
    print_fail "compose wiring missing payment env or cert mount: ${compose_file}"
  fi
done

echo

for cert in "${TARGET_CERTS[@]}"; do
  if [[ -f "${cert}" ]]; then
    print_ok "certificate exists: ${cert}"
  else
    print_fail "certificate missing: ${cert}"
  fi
done

echo

required_wx_vars=(
  WXPAY_APP_ID
  WXPAY_MINI_APP_ID
  WXPAY_OFFICIAL_APP_ID
  WXPAY_MCH_ID
  WXPAY_API_KEY
  WXPAY_MCH_KEY
)

for var_name in "${required_wx_vars[@]}"; do
  if [[ -n "${!var_name:-}" ]]; then
    print_ok "env set: ${var_name}"
  else
    print_fail "env missing: ${var_name}"
  fi
done

echo

if [[ "${ALIPAY_ENABLED:-false}" == "true" ]]; then
  for var_name in ALIPAY_APP_ID ALIPAY_PRIVATE_KEY ALIPAY_PUBLIC_KEY ALIPAY_GATEWAY_URL; do
    if [[ -n "${!var_name:-}" ]]; then
      print_ok "env set: ${var_name}"
    else
      print_fail "env missing: ${var_name}"
    fi
  done
else
  print_warn "ALIPAY_ENABLED is not true; Alipay pay/refund will stay disabled"
fi

echo

if [[ -n "${WXPAY_MCH_ID:-}" ]]; then
  cert_to_verify="${ROOT_DIR}/deploy/docker/certs/wxpay/apiclient_cert.p12"
  if [[ -f "${cert_to_verify}" ]]; then
    if openssl pkcs12 -in "${cert_to_verify}" -passin "pass:${WXPAY_MCH_ID}" -nokeys -clcerts >/dev/null 2>&1; then
      print_ok "certificate password matches WXPAY_MCH_ID"
    else
      print_fail "certificate password check failed for WXPAY_MCH_ID"
    fi
  fi
else
  print_warn "skip certificate password check because WXPAY_MCH_ID is empty"
fi

echo

if [[ "${missing}" -ne 0 ]]; then
  echo "Payment configuration is incomplete."
  exit 1
fi

echo "Payment configuration looks ready."
