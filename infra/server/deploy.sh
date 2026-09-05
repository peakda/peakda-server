#!/bin/bash
# peakda 배포 스크립트. GitHub Actions 가 SSM Send-Command 로 호출한다.
#
#   사용법: deploy.sh <image-tag>
#
# 하는 일: 자산 최신화 → SSM 에서 .env 생성 → 이미지 pull → 기동 → 헬스 확인.
# 헬스체크가 실패하면 직전에 돌던 이미지로 되돌린다.
set -euo pipefail

APP_DIR=/opt/peakda
TAG="${1:-latest}"

cd "$APP_DIR"
# shellcheck source=/dev/null
source "$APP_DIR/env.conf"

log() { echo "[deploy] $(date -Is) $*"; }

# ---------------------------------------------------------------------------
# 1. 서버 자산 최신화
#    compose 파일이나 Caddyfile 을 고쳐 S3 에 올리면 다음 배포에서 자동 반영된다.
# ---------------------------------------------------------------------------
log "자산 동기화"
aws s3 sync "s3://$ASSETS_BUCKET/assets/" "$APP_DIR/" --region "$AWS_REGION" --quiet
chmod +x "$APP_DIR"/*.sh

# ---------------------------------------------------------------------------
# 2. 롤백 대비 — 지금 돌고 있는 이미지를 기억해 둔다
# ---------------------------------------------------------------------------
PREVIOUS_IMAGE=""
if docker inspect peakda-app >/dev/null 2>&1; then
  PREVIOUS_IMAGE="$(docker inspect --format '{{.Config.Image}}' peakda-app)"
  log "현재 이미지: $PREVIOUS_IMAGE"
fi

# ---------------------------------------------------------------------------
# 3. SSM Parameter Store 에서 .env 생성
#    값에 개행이 없다는 전제(환경변수이므로 성립). CLI v2 가 페이지네이션을 자동 처리한다.
# ---------------------------------------------------------------------------
log "환경변수 로드: $PARAMETER_PREFIX"
aws ssm get-parameters-by-path \
  --path "$PARAMETER_PREFIX" \
  --with-decryption \
  --recursive \
  --region "$AWS_REGION" \
  --output json \
  | jq -r '.Parameters[] | "\(.Name | split("/") | last)=\(.Value)"' > "$APP_DIR/.env.new"

if [ ! -s "$APP_DIR/.env.new" ]; then
  log "ERROR: SSM 파라미터를 하나도 읽지 못했다. 배포를 중단한다."
  rm -f "$APP_DIR/.env.new"
  exit 1
fi

# 아직 값이 주입되지 않은 시크릿이 있으면 앱이 이상하게 뜨므로 미리 막는다.
if grep -q 'PLACEHOLDER_SET_ME_VIA_CLI' "$APP_DIR/.env.new"; then
  log "ERROR: 값이 주입되지 않은 시크릿이 있다:"
  grep -l 'PLACEHOLDER_SET_ME_VIA_CLI' "$APP_DIR/.env.new" >/dev/null
  grep 'PLACEHOLDER_SET_ME_VIA_CLI' "$APP_DIR/.env.new" | cut -d= -f1 | sed 's/^/  - /'
  log "aws ssm put-parameter --name $PARAMETER_PREFIX/<KEY> --value <값> --type SecureString --overwrite"
  rm -f "$APP_DIR/.env.new"
  exit 1
fi

echo "ECR_IMAGE=$ECR_REPOSITORY:$TAG" >> "$APP_DIR/.env.new"
mv "$APP_DIR/.env.new" "$APP_DIR/.env"
chmod 600 "$APP_DIR/.env"

# ---------------------------------------------------------------------------
# 4. ECR 로그인 후 pull
# ---------------------------------------------------------------------------
log "ECR 로그인"
aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "${ECR_REPOSITORY%%/*}"

log "이미지 pull: $ECR_REPOSITORY:$TAG"
docker compose pull

# ---------------------------------------------------------------------------
# 5. 기동
# ---------------------------------------------------------------------------
log "컨테이너 기동"
docker compose up -d --remove-orphans

# ---------------------------------------------------------------------------
# 6. 헬스 확인 (최대 150초)
# ---------------------------------------------------------------------------
log "헬스체크 대기"
HEALTHY=false
for _ in $(seq 1 30); do
  status="$(docker inspect --format '{{.State.Health.Status}}' peakda-app 2>/dev/null || echo starting)"
  if [ "$status" = "healthy" ]; then
    HEALTHY=true
    break
  fi
  if [ "$status" = "unhealthy" ]; then
    break
  fi
  sleep 5
done

if [ "$HEALTHY" != true ]; then
  log "ERROR: 헬스체크 실패. 최근 로그:"
  docker compose logs --tail 50 app || true

  if [ -n "$PREVIOUS_IMAGE" ]; then
    log "롤백: $PREVIOUS_IMAGE"
    sed -i "s|^ECR_IMAGE=.*|ECR_IMAGE=$PREVIOUS_IMAGE|" "$APP_DIR/.env"
    docker compose up -d app
    log "롤백 완료"
  else
    log "이전 이미지가 없어 롤백하지 않는다(최초 배포)"
  fi
  exit 1
fi

log "배포 성공: $ECR_REPOSITORY:$TAG"

# ---------------------------------------------------------------------------
# 7. 디스크 정리
#    배포마다 ~400MB 이미지가 쌓인다. 실측상 가장 빠른 디스크 소비원이다.
# ---------------------------------------------------------------------------
docker image prune -af --filter "until=168h" >/dev/null 2>&1 || true
log "디스크 사용률: $(df -h / | awk 'NR==2 {print $5}')"
