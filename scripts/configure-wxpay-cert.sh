#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOURCE_CERT="${1:-}"

if [[ -z "${SOURCE_CERT}" ]]; then
  cat >&2 <<'USAGE'
Usage:
  scripts/configure-wxpay-cert.sh /path/to/apiclient_cert.p12

This copies the WeChat Pay merchant API certificate into every deploy
directory used by this repository. The certificate file is ignored by git.
USAGE
  exit 64
fi

if [[ ! -f "${SOURCE_CERT}" ]]; then
  echo "Certificate not found: ${SOURCE_CERT}" >&2
  exit 66
fi

case "$(basename "${SOURCE_CERT}")" in
  apiclient_cert.p12) ;;
  *)
    echo "Certificate filename must be apiclient_cert.p12" >&2
    exit 65
    ;;
esac

TARGET_DIRS=(
  "${ROOT_DIR}/deploy/docker/certs/wxpay"
  "${ROOT_DIR}/deploy-lighthouse/certs/wxpay"
  "${ROOT_DIR}/deploy-lighthouse-light/certs/wxpay"
)

for dir in "${TARGET_DIRS[@]}"; do
  mkdir -p "${dir}"
  install -m 0600 "${SOURCE_CERT}" "${dir}/apiclient_cert.p12"
  echo "Installed ${dir}/apiclient_cert.p12"
done

echo
echo "Done. Container path is /opt/shiyiju/certs/wxpay/apiclient_cert.p12"
