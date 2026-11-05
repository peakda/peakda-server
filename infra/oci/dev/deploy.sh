#!/bin/bash
# 사용법: deploy.sh <커밋 SHA> [서버에 pull된 GHCR 이미지]
set -euo pipefail
cd /opt/peakda-dev
umask 077
exec 9>.deploy.lock
flock -n 9 || { echo "Development deployment already running" >&2; exit 1; }

sha="${1:?Usage: deploy.sh <commit SHA> [image]}"
[[ "$sha" =~ ^[a-f0-9]{40}$ ]] || { echo "Invalid commit SHA" >&2; exit 1; }
image="${2:-peakda-dev-local:$sha}"
if [[ "$image" != "peakda-dev-local:$sha" && ! "$image" =~ ^ghcr\.io/peakda/peakda-server@sha256:[a-f0-9]{64}$ ]]; then
    echo "Invalid development image" >&2
    exit 1
fi

revision="$(docker image inspect --format '{{index .Config.Labels "org.opencontainers.image.revision"}}' "$image")"
[[ "$revision" == "$sha" ]] || { echo "Image revision does not match $sha" >&2; exit 1; }
previous="$(grep '^APP_IMAGE=' .env)"
[[ "$previous" != *$'\n'* && -n "${previous#APP_IMAGE=}" ]] || { echo "Expected one APP_IMAGE in .env" >&2; exit 1; }

set_image() {
    local tmp
    tmp="$(mktemp .env.XXXXXX)"
    awk -v line="$1" '/^APP_IMAGE=/{print line; next} {print}' .env > "$tmp"
    mv -f "$tmp" .env
}

start_app() {
    docker compose --project-name peakda-dev up -d --no-deps --pull never --wait --wait-timeout 180 app || return
    curl -fsS --max-time 8 http://127.0.0.1:18081/actuator/health/readiness
}

set_image "APP_IMAGE=$image"
if ! start_app; then
    echo "Development readiness failed. Restoring the previous image." >&2
    set_image "$previous"
    start_app
    exit 1
fi
printf '\nDeployed %s\n' "$sha"
