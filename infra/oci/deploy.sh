#!/bin/bash
# peakda prod 배포. GitHub Actions 가 배포 전용 SSH 키로 deploy-gate.sh 를 거쳐 호출한다.
#
#   사용법: deploy.sh <커밋 SHA> <GitHub actor>    (GITHUB_TOKEN 은 stdin 으로 받는다)
#
# 하는 일: 그 커밋의 서버 자산(compose·Caddyfile·미디어 캐시 설정·Alloy 설정·backup.sh) 반영 → GHCR 이미지 pull → 기동 → 헬스 확인.
# 헬스체크가 실패하면 직전 이미지로 되돌린다. 이 파일과 deploy-gate.sh 는 스스로 바꾸지 않는다
# (실행 중인 스크립트를 덮어쓰지 않기 위해서다. 바뀌면 operations.md 의 "스크립트 갱신" 대로 올린다).
set -euo pipefail

APP_DIR=/opt/peakda
REPO=peakda/peakda-server
IMAGE_REPO=ghcr.io/peakda/peakda-server
SHA="${1:?커밋 SHA 가 필요하다}"
ACTOR="${2:?GitHub actor 가 필요하다}"

log() { echo "[deploy] $(date -Is) $*"; }
die() {
  log "ERROR: $*"
  exit 1
}

[[ "$SHA" =~ ^[0-9a-f]{40}$ ]] || die "커밋 SHA 형식이 아니다: $SHA"
cd "$APP_DIR"
[[ -f .env ]] || die ".env 가 없다. operations.md 의 초기 설정을 먼저 한다"

TOKEN=""
read -r TOKEN || true

# 토큰을 명령행 인자로 넘기면 같은 서버의 다른 사용자가 프로세스 목록에서 읽을 수 있다.
# 임시 디렉터리(700)의 파일로만 다루고 끝나면 지운다. docker 로그인 정보도 여기에 둔다.
umask 077
SECRET_DIR="$(mktemp -d)"
trap 'rm -rf "$SECRET_DIR"' EXIT
AUTH_HEADER=()
if [[ -n "$TOKEN" ]]; then
  printf 'Authorization: Bearer %s\n' "$TOKEN" > "$SECRET_DIR/auth-header"
  AUTH_HEADER=(-H "@$SECRET_DIR/auth-header")
fi

# ---------------------------------------------------------------------------
# 1. 서버 자산을 그 커밋 기준으로 맞춘다
# ---------------------------------------------------------------------------
log "자산 반영: $SHA"
ASSETS=(docker-compose.yml Caddyfile media-cache.conf.template backup.sh)
# 나중에 생긴 파일. 그 전 커밋으로 되돌려 배포할 때도 멈추지 않도록, 받지 못하면 있던 파일을 그대로 둔다.
OPTIONAL_ASSETS=(alloy-config.alloy)

fetch_asset() {
  curl -fsSL "${AUTH_HEADER[@]}" \
    "https://raw.githubusercontent.com/$REPO/$SHA/infra/oci/$1" -o "$1.new"
}
for file in "${ASSETS[@]}"; do
  fetch_asset "$file"
done
for file in "${OPTIONAL_ASSETS[@]}"; do
  fetch_asset "$file" || { log "WARN: $file 을 받지 못해 있던 파일을 그대로 쓴다"; rm -f "$file.new"; }
done
chmod +x backup.sh.new

# 잘못된 Caddyfile 이 디스크에 남으면 다음에 Caddy 가 재시작될 때(서버 재부팅 등) HTTPS 가 통째로 내려간다.
# 아무것도 바꾸기 전에 떠 있는 Caddy 로 검증한다.
if docker inspect -f '{{.State.Running}}' peakda-caddy 2> /dev/null | grep -q true; then
  validation="$(docker exec -i peakda-caddy caddy validate --adapter caddyfile --config /dev/stdin < Caddyfile.new 2>&1)" \
    || die "새 Caddyfile 이 유효하지 않아 배포를 멈춘다: $(tail -c 500 <<< "$validation")"
fi

# 컨테이너에 파일 하나로 바인드 마운트되는 설정. mv 로 바꾸면 컨테이너는 옛 파일(inode)을 계속 보므로
# 내용을 제자리에 덮어쓴다. 그 밖의 파일은 mv 한다(cron 이 실행 중인 backup.sh 를 덮어쓰면 깨진다).
MOUNTED_ASSETS=(Caddyfile media-cache.conf.template alloy-config.alloy)
for file in "${ASSETS[@]}" "${OPTIONAL_ASSETS[@]}"; do
  [[ -f "$file.new" ]] || continue
  # compose 는 없는 파일을 바인드 마운트하면 그 자리에 빈 디렉터리를 만든다. 먼저 치운다.
  if [[ -d "$file" ]]; then rmdir "$file"; fi
  if [[ -f "$file" && " ${MOUNTED_ASSETS[*]} " == *" $file "* ]]; then
    cat "$file.new" > "$file"
    # 자르고 쓰는 방식이라 디스크가 가득 차면 빈 설정이 남을 수 있다.
    cmp -s "$file.new" "$file" || die "$file 을 끝까지 쓰지 못했다"
    rm -f "$file.new"
  else
    mv "$file.new" "$file"
  fi
done

# 마운트된 설정을 컨테이너에 반영했는지는 반영에 성공했을 때만 쓰는 해시로 판단한다.
# "새 파일과 디스크 파일이 다른가" 로 보면, 반영 전에 배포가 실패했을 때 다음 배포가 반영을 건너뛴다.
APPLIED_DIR="$APP_DIR/.applied"
mkdir -p "$APPLIED_DIR"
needs_apply() { [[ -f "$1" && "$(sha256sum < "$1")" != "$(cat "$APPLIED_DIR/$1.sha256" 2> /dev/null)" ]]; }
mark_applied() { sha256sum < "$1" > "$APPLIED_DIR/$1.sha256"; }

# ---------------------------------------------------------------------------
# 2. 이미지 pull. 로그인 정보는 임시 디렉터리에만 두고 지운다
# ---------------------------------------------------------------------------
IMAGE="$IMAGE_REPO:$SHA"
PREVIOUS_IMAGE="$(sed -n 's/^APP_IMAGE=//p' .env)"
log "현재 이미지: ${PREVIOUS_IMAGE:-없음}"

export DOCKER_CONFIG="$SECRET_DIR/docker"
mkdir -p "$DOCKER_CONFIG"
if [[ -n "$TOKEN" ]]; then
  printf '%s' "$TOKEN" | docker login ghcr.io -u "$ACTOR" --password-stdin > /dev/null
fi
log "이미지 pull: $IMAGE"
docker pull --quiet "$IMAGE" > /dev/null

# APP_IMAGE 는 .env 에서 이 스크립트가 관리하는 유일한 줄이다.
set_image() {
  local tmp
  tmp="$(mktemp "$APP_DIR/.env.XXXXXX")"
  grep -v '^APP_IMAGE=' .env > "$tmp" || true
  echo "APP_IMAGE=$1" >> "$tmp"
  chmod 600 "$tmp"
  mv "$tmp" .env
}
set_image "$IMAGE"

# ---------------------------------------------------------------------------
# 3. 기동과 헬스 확인 (최대 약 4분)
# ---------------------------------------------------------------------------
log "컨테이너 기동"
docker compose up -d --remove-orphans

healthy=false
for _ in $(seq 1 48); do
  status="$(docker inspect --format '{{.State.Health.Status}}' peakda-app 2> /dev/null || echo starting)"
  [[ "$status" == healthy ]] && healthy=true && break
  [[ "$status" == unhealthy ]] && break
  sleep 5
done

if [[ "$healthy" != true ]]; then
  log "ERROR: 헬스체크 실패. 최근 로그:"
  docker compose logs --tail 60 app || true
  if [[ -n "$PREVIOUS_IMAGE" && "$PREVIOUS_IMAGE" != "$IMAGE" ]]; then
    log "롤백: $PREVIOUS_IMAGE"
    set_image "$PREVIOUS_IMAGE"
    docker compose up -d app
  fi
  exit 1
fi

# 마운트된 설정을 각 컨테이너에 반영한다. compose 는 파일 내용이 바뀐 것을 모른다.
config_failed=false

# 재시작한 컨테이너가 몇 초 뒤에도 떠 있는지 본다. 설정이 틀리면 기동 직후 죽는다.
restart_and_check() {
  local service="$1" container="$2"
  docker compose restart "$service" > /dev/null 2>&1 || return 1
  sleep 5
  [[ "$(docker inspect -f '{{.State.Running}} {{.State.Restarting}}' "$container" 2> /dev/null)" == "true false" ]]
}

if needs_apply Caddyfile; then
  # 설정만 다시 읽는다(인증서·연결 유지).
  if out="$(docker exec peakda-caddy caddy reload --config /etc/caddy/Caddyfile 2>&1)"; then
    # 예전 deploy.sh 는 mv 로 바꿨으므로 컨테이너가 지워진 옛 inode 를 보고 있을 수 있다. 그러면 reload 는
    # 옛 내용을 다시 읽고 성공한다. 컨테이너 안 파일이 다르면 재시작해 마운트를 다시 잡는다(검증은 위에서 했다).
    if [[ "$(docker exec peakda-caddy sha256sum /etc/caddy/Caddyfile | cut -d' ' -f1)" != "$(sha256sum < Caddyfile | cut -d' ' -f1)" ]]; then
      log "Caddy 가 옛 설정 파일을 보고 있어 재시작한다"
      restart_and_check caddy peakda-caddy && mark_applied Caddyfile \
        || { log "ERROR: Caddy 재시작 실패"; config_failed=true; }
    else
      mark_applied Caddyfile
    fi
  else
    log "ERROR: Caddy 설정 반영 실패. 옛 설정으로 돌고 있다: $(tail -c 500 <<< "$out")"
    config_failed=true
  fi
fi
# nginx 템플릿은 기동할 때만 렌더링되고, Alloy 는 재시작해야 설정을 다시 읽는다.
if needs_apply media-cache.conf.template; then
  restart_and_check media-cache peakda-media-cache && mark_applied media-cache.conf.template \
    || { log "ERROR: 미디어 캐시가 새 설정으로 뜨지 않았다(cdn 확인 필요)"; config_failed=true; }
fi
if needs_apply alloy-config.alloy; then
  restart_and_check alloy peakda-alloy && mark_applied alloy-config.alloy \
    || { log "ERROR: 수집기가 새 설정으로 뜨지 않았다"; config_failed=true; }
fi

if [[ "$config_failed" == true ]]; then
  log "앱은 배포했지만 설정 반영에 실패했다: $IMAGE"
else
  log "배포 성공: $IMAGE"
fi

# 배포마다 이미지가 쌓인다. 일주일 넘은 이미지만 지운다(직전 이미지는 롤백용으로 남는다).
docker image prune -af --filter "until=168h" > /dev/null 2>&1 || true
log "디스크 사용률: $(df -h / | awk 'NR == 2 { print $5 }')"

if [[ "$config_failed" == true ]]; then exit 1; fi
