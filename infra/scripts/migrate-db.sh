#!/bin/bash
# Railway PostgreSQL 데이터를 AWS EC2 의 PostgreSQL 로 옮긴다.
#
#   사용법:
#     export RAILWAY_DATABASE_URL='postgresql://user:pass@host:port/railway'
#     ./migrate-db.sh [dev] [--wipe]
#
#   --wipe  복원 전 대상 DB 의 기존 데이터를 지운다.
#           새 환경이라 이미 스케줄러가 수집한 데이터와 테스트 로그인 계정이
#           들어 있으므로, 첫 이관에는 이 옵션을 쓰는 편이 깔끔하다.
#
# 흐름: Railway 덤프 → S3 업로드 → SSM 으로 EC2 안에서 복원.
# 터널을 유지하지 않아 대용량에도 안정적이고, 복원이 서버 안에서 일어난다.
#
# 값은 Railway 대시보드 → Postgres → Variables 의 DATABASE_PUBLIC_URL 을 쓴다
# (내부 URL 은 Railway 네트워크 밖에서 접속되지 않는다).
set -euo pipefail

: "${RAILWAY_DATABASE_URL:?RAILWAY_DATABASE_URL 을 설정하세요}"

ENV="${1:-dev}"
WIPE="${2:-}"
REGION="${AWS_REGION:-ap-northeast-2}"
STAMP="$(date -u +%Y%m%d-%H%M%S)"
DUMP="/tmp/railway-$STAMP.dump"
S3_KEY="migration/railway-$STAMP.dump"

trap 'rm -f "$DUMP"' EXIT

log() { echo "[migrate-db] $*"; }

# 덤프 경유지는 백업·자산 버킷(-storage-)이다. 앱 이미지 버킷(-media-)이 아니다.
ENV_DIR="$(cd "$(dirname "$0")/../envs/$ENV" && pwd)"
BUCKET="$(cd "$ENV_DIR" && terraform output -raw storage_bucket)"
INSTANCE_ID="$(cd "$ENV_DIR" && terraform output -raw instance_id)"

log "대상 인스턴스: $INSTANCE_ID"
log "경유 버킷: $BUCKET"

# ---------------------------------------------------------------------------
# 1. Railway 덤프
#    로컬에 pg_dump 가 없으면 docker 로 대체한다. 서버보다 낮은 버전의
#    pg_dump 는 실패하므로 postgres:18 이미지를 쓴다.
# ---------------------------------------------------------------------------
# Liquibase 관리 테이블은 덤프에서 뺀다. 대상 DB 에는 이미 적용 이력이 있고,
# 덮어쓰면 changelog 재적용이 일어나 스키마가 깨진다.
# scheduler_job_runs 는 실행 이력이라 새 환경에서 새로 쌓으면 된다.
DUMP_EXCLUDES=(
  --exclude-table=databasechangelog
  --exclude-table=databasechangeloglock
  --exclude-table=scheduler_job_runs
)

log "Railway 덤프 시작"
if command -v pg_dump >/dev/null 2>&1; then
  pg_dump -Fc --no-owner --no-acl "${DUMP_EXCLUDES[@]}" "$RAILWAY_DATABASE_URL" > "$DUMP"
else
  log "pg_dump 가 없어 docker(postgres:18)로 실행한다"
  docker run --rm -i postgres:18 \
    pg_dump -Fc --no-owner --no-acl "${DUMP_EXCLUDES[@]}" "$RAILWAY_DATABASE_URL" > "$DUMP"
fi

if [ ! -s "$DUMP" ]; then
  log "ERROR: 덤프가 비어 있다. RAILWAY_DATABASE_URL 을 확인하세요."
  exit 1
fi
log "덤프 완료: $(du -h "$DUMP" | cut -f1)"

# ---------------------------------------------------------------------------
# 2. S3 업로드
# ---------------------------------------------------------------------------
log "업로드: s3://$BUCKET/$S3_KEY"
aws s3 cp "$DUMP" "s3://$BUCKET/$S3_KEY" --region "$REGION" --quiet

# ---------------------------------------------------------------------------
# 3. EC2 에서 복원
# ---------------------------------------------------------------------------
log "복원 명령 전송"
CMD_ID="$(aws ssm send-command \
  --instance-ids "$INSTANCE_ID" \
  --document-name AWS-RunShellScript \
  --comment "restore $S3_KEY" \
  --parameters "commands=['/opt/peakda/restore.sh $S3_KEY $WIPE']" \
  --timeout-seconds 1800 \
  --region "$REGION" \
  --query 'Command.CommandId' --output text)"

log "SSM command: $CMD_ID"

for _ in $(seq 1 90); do
  sleep 10
  STATUS="$(aws ssm get-command-invocation --command-id "$CMD_ID" \
    --instance-id "$INSTANCE_ID" --region "$REGION" \
    --query Status --output text 2>/dev/null || echo Pending)"
  printf '.'
  case "$STATUS" in
    Success|Failed|Cancelled|TimedOut) break ;;
  esac
done
echo

log "상태: $STATUS"
aws ssm get-command-invocation --command-id "$CMD_ID" --instance-id "$INSTANCE_ID" \
  --region "$REGION" --query StandardOutputContent --output text

if [ "$STATUS" != "Success" ]; then
  log "오류 출력:"
  aws ssm get-command-invocation --command-id "$CMD_ID" --instance-id "$INSTANCE_ID" \
    --region "$REGION" --query StandardErrorContent --output text
  exit 1
fi

log "이관 완료. 덤프는 s3://$BUCKET/$S3_KEY 에 남아 있다."
