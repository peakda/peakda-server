#!/bin/bash
# 배포 전용 SSH 키의 forced command. authorized_keys 에서 이렇게 묶는다.
#   restrict,command="/opt/peakda/deploy-gate.sh" ssh-ed25519 AAAA... peakda-deploy
#
# "deploy <40자리 커밋 SHA> <GitHub actor>" 만 허용하고 deploy.sh 로 넘긴다.
# 키가 새어도 셸을 얻거나 임의 명령을 실행할 수 없다. GHCR 토큰은 stdin 으로 그대로 흘려보낸다.
set -euo pipefail

read -r cmd sha actor extra <<< "${SSH_ORIGINAL_COMMAND:-}"

if [[ "$cmd" != deploy || ! "$sha" =~ ^[0-9a-f]{40}$ || ! "${actor:-}" =~ ^[A-Za-z0-9][A-Za-z0-9-]*(\[bot\])?$ || -n "${extra:-}" ]]; then
  echo "허용되지 않은 명령이다: deploy <sha> <actor> 만 받는다" >&2
  exit 1
fi

exec /opt/peakda/deploy.sh "$sha" "$actor"
