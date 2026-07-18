#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_DIR="${DEPLOY_DIR:-${ROOT_DIR}/deploy-lighthouse}"
REMOTE_HOST="${REMOTE_HOST:-}"
REMOTE_PORT="${REMOTE_PORT:-22}"
REMOTE_USER="${REMOTE_USER:-root}"
REMOTE_DIR="${REMOTE_DIR:-/opt/shiyiju}"
VERIFY_BASE_URL="${VERIFY_BASE_URL:-https://www.art1.cn}"
SKIP_REMOTE_RESTART="${SKIP_REMOTE_RESTART:-false}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing command: $1" >&2
    exit 127
  fi
}

print_step() {
  printf '\n==> %s\n' "$1"
}

print_ok() {
  printf '[OK] %s\n' "$1"
}

print_fail() {
  printf '[FAIL] %s\n' "$1" >&2
  exit 1
}

usage() {
  cat <<EOF
Usage:
  REMOTE_HOST=YOUR_SERVER_IP $0

Optional env:
  REMOTE_USER=root
  REMOTE_PORT=22
  REMOTE_DIR=/opt/shiyiju
  DEPLOY_DIR=${ROOT_DIR}/deploy-lighthouse
  VERIFY_BASE_URL=https://www.art1.cn
  SKIP_REMOTE_RESTART=false
EOF
}

[[ -n "${REMOTE_HOST}" ]] || {
  usage
  print_fail "REMOTE_HOST is required"
}

require_cmd rsync
require_cmd ssh

"${ROOT_DIR}/scripts/verify-legal-pages.sh" "${DEPLOY_DIR}" || print_fail "local legal pages verification failed"

remote_target="${REMOTE_USER}@${REMOTE_HOST}"
ssh_args=(-p "${REMOTE_PORT}")
rsync_ssh="ssh -p ${REMOTE_PORT}"

print_step "Ensuring remote directories exist"
ssh "${ssh_args[@]}" "${remote_target}" \
  "mkdir -p '${REMOTE_DIR}/frontend-h5/agreement' '${REMOTE_DIR}/frontend-h5/privacy' '${REMOTE_DIR}/config'" \
  || print_fail "failed to create remote directories"
print_ok "remote directories ready"

print_step "Uploading legal page artifacts"
rsync -avz --delete -e "${rsync_ssh}" \
  "${DEPLOY_DIR}/frontend-h5/agreement/" \
  "${remote_target}:${REMOTE_DIR}/frontend-h5/agreement/" \
  || print_fail "failed to upload agreement assets"

rsync -avz --delete -e "${rsync_ssh}" \
  "${DEPLOY_DIR}/frontend-h5/privacy/" \
  "${remote_target}:${REMOTE_DIR}/frontend-h5/privacy/" \
  || print_fail "failed to upload privacy assets"

rsync -avz -e "${rsync_ssh}" \
  "${DEPLOY_DIR}/config/nginx-h5.conf" \
  "${remote_target}:${REMOTE_DIR}/config/nginx-h5.conf" \
  || print_fail "failed to upload nginx config"
print_ok "remote files uploaded"

if [[ "${SKIP_REMOTE_RESTART}" != "true" ]]; then
  print_step "Restarting remote frontend-h5 service"
  ssh "${ssh_args[@]}" "${remote_target}" \
    "cd '${REMOTE_DIR}' && docker compose restart frontend-h5" \
    || print_fail "failed to restart frontend-h5"
  print_ok "frontend-h5 restarted"
fi

print_step "Verifying remote legal pages"
BASE_URL="${VERIFY_BASE_URL}" "${ROOT_DIR}/scripts/verify-legal-pages.sh" "${DEPLOY_DIR}" \
  || print_fail "remote legal pages verification failed"

print_ok "legal page deployment finished"
