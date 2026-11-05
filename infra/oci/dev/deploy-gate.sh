#!/bin/bash
# 개발 전용 SSH 키에 restrict,command="/opt/peakda-dev/deploy-gate.sh"를 설정한다.
set -euo pipefail

command_line="${SSH_ORIGINAL_COMMAND:-}"
read -r action sha digest actor extra <<< "$command_line"
if [[ "$command_line" == *$'\n'* || "$action" != deploy || ! "$sha" =~ ^[a-f0-9]{40}$ ||
      ! "$digest" =~ ^sha256:[a-f0-9]{64}$ ||
      ! "$actor" =~ ^[A-Za-z0-9][A-Za-z0-9-]*(\[bot\])?$ || -n "$extra" ]]; then
    echo "Usage: deploy <commit SHA> <image digest> <GitHub actor>" >&2
    exit 1
fi

read -r -t 10 token
[[ -n "$token" ]] || { echo "Missing GHCR token" >&2; exit 1; }

image="ghcr.io/peakda/peakda-server@$digest"
(
    umask 077
    DOCKER_CONFIG="$(mktemp -d)"
    export DOCKER_CONFIG
    trap 'rm -rf "$DOCKER_CONFIG"' EXIT
    printf '%s\n' "$token" | docker login ghcr.io -u "$actor" --password-stdin >/dev/null
    docker pull --quiet "$image"
)
unset token
exec /opt/peakda-dev/deploy.sh "$sha" "$image"
