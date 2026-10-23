#!/usr/bin/env bash
# DanaSea production deploy entrypoint (runs on the home server).
#
# Installed as a root-owned forced SSH command so the CI key can do nothing else:
#   sudo install -m 755 scripts/deploy.sh /usr/local/bin/danasea-deploy
#   ~/.ssh/authorized_keys:
#     command="/usr/local/bin/danasea-deploy",restrict ssh-ed25519 AAAA... github-actions-deploy
#
# Usage (via SSH or locally): deploy <40-char git sha>
set -Eeuo pipefail
umask 077

APP_DIR="${APP_DIR:-$HOME/danasea}"
COMPOSE_FILE="docker-compose.prod.yml"
STATE_FILE="$APP_DIR/.deploy-state"
BACKUP_DIR="${BACKUP_DIR:-$HOME/backups/danasea}"
BACKUP_KEEP=7
WAIT_TIMEOUT=300

log() { printf '[%s] %s\n' "$(date '+%F %T')" "$*"; }

compose() {
  local tag="$1"; shift
  IMAGE_TAG="$tag" docker compose -f "$COMPOSE_FILE" "$@"
}

# --- Validate input ("deploy <sha>" from SSH_ORIGINAL_COMMAND or CLI args) ---
read -r action tag _ <<< "${SSH_ORIGINAL_COMMAND:-$*}" || true
if [[ "${SSH_ORIGINAL_COMMAND:-$*}" != "deploy ${tag:-}" || "${action:-}" != "deploy" || ! "${tag:-}" =~ ^[0-9a-f]{40}$ ]]; then
  echo "Usage: deploy <40-char git sha>" >&2
  exit 2
fi

cd "$APP_DIR"

# --- Prevent concurrent deploys ---
exec 9>"$APP_DIR/.deploy.lock"
flock -n 9 || { echo "Another deploy is in progress" >&2; exit 1; }

prev_tag="$(cat "$STATE_FILE" 2>/dev/null || true)"
if [[ -n "$prev_tag" && ! "$prev_tag" =~ ^[0-9a-f]{40}$ ]]; then
  echo "Invalid previous release state" >&2
  exit 2
fi
log "Deploying ${tag:0:7} (previous: ${prev_tag:0:7})"

rollback() {
  if [[ -z "$prev_tag" ]]; then
    log "No previous release to roll back to"
    return
  fi
  log "Rolling back to ${prev_tag:0:7}"
  git checkout --quiet --force --detach "$prev_tag"
  compose "$prev_tag" up -d --wait --wait-timeout "$WAIT_TIMEOUT" || log "Rollback failed, manual action required"
}

# --- 1. Sync compose/config files to the exact commit being deployed ---
git fetch --quiet origin main
git cat-file -e "${tag}^{commit}"
git merge-base --is-ancestor "$tag" origin/main || { echo "Release is not on origin/main" >&2; exit 1; }
git checkout --quiet --force --detach "$tag"

# Fail before changing running services when private files, keys or the subnet are not ready.
python3 scripts/production.py check

# --- 2. Backup database before touching anything ---
if [[ -n "$(compose "$tag" ps -q postgres 2>/dev/null)" ]]; then
  mkdir -p "$BACKUP_DIR"
  backup_file="$BACKUP_DIR/pre-${tag:0:7}-$(date +%Y%m%d%H%M%S).sql.gz"
  log "Backing up database -> $backup_file"
  compose "$tag" exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB"' | gzip > "$backup_file"
  ls -1t "$BACKUP_DIR"/pre-*.sql.gz | tail -n +$((BACKUP_KEEP + 1)) | xargs -r rm --
fi

# --- 3. Pull prebuilt images (no building on this 4GB box) ---
log "Pulling images"
compose "$tag" pull

# --- 4. Start and wait for healthchecks; roll back on failure ---
log "Starting containers"
if ! compose "$tag" up -d --wait --wait-timeout "$WAIT_TIMEOUT" --remove-orphans; then
  log "Deploy failed healthchecks"
  compose "$tag" logs --tail 50 backend || true
  rollback
  exit 1
fi

echo "$tag" > "$STATE_FILE"

# --- 5. Clean up images unused for a week (running + recent ones are kept for rollback) ---
docker image prune -af --filter "until=168h" > /dev/null

log "Deploy ${tag:0:7} succeeded"
