#!/bin/bash
# PostgreSQL·Redis 백업. ubuntu 사용자의 crontab 이 6시간마다 실행한다.
#   0 */6 * * * /opt/peakda/backup.sh >> /opt/peakda/logs/backup.log 2>&1
#
# OCI Object Storage 백업 버킷(.env 의 BACKUP_BUCKET)에 S3 호환 API 로 올린다.
# 보관 기간은 버킷 수명 주기 규칙(30일)이 정한다. 여기서는 지우지 않는다.
# 관리형 DB 의 시점 복구가 없으므로 이 주기(6시간)가 최대 유실 범위다.
set -euo pipefail

APP_DIR=/opt/peakda
cd "$APP_DIR"

log() { echo "[backup] $(date -Is) $*"; }

env_value() { sed -n "s/^$1=//p" .env | tail -1; }

BUCKET="$(env_value BACKUP_BUCKET)"
ENDPOINT="$(env_value STORAGE_ENDPOINT)"
REGION="$(env_value STORAGE_REGION)"
[[ -n "$BUCKET" && -n "$ENDPOINT" && -n "$REGION" ]] || { log "ERROR: .env 에 BACKUP_BUCKET·STORAGE_ENDPOINT·STORAGE_REGION 이 필요하다"; exit 1; }

TS="$(date -u +%Y%m%d-%H%M%S)"
WORK="$(mktemp -d "$APP_DIR/data/backup.XXXXXX")"
trap 'rm -rf "$WORK"' EXIT

# -Fc 는 자체 압축된 custom format 이다.
log "pg_dump 시작"
docker exec peakda-postgres pg_dump -U peakda -Fc --no-owner --no-acl peakda > "$WORK/peakda-$TS.dump"

# 리프레시 토큰이 Redis 에 있다. 복제 프로토콜로 RDB 스냅샷을 받는다.
log "Redis 스냅샷"
docker exec peakda-redis sh -c 'redis-cli --rdb /tmp/backup.rdb > /dev/null && cat /tmp/backup.rdb && rm -f /tmp/backup.rdb' > "$WORK/redis-$TS.rdb"

log "업로드: $(du -ch "$WORK"/* | tail -1 | cut -f1)"
# 자격증명을 명령행(-e 키=값)으로 넘기면 같은 서버의 다른 사용자가 프로세스 목록에서 읽을 수 있다.
# 600 권한의 임시 파일로 넘긴다. OCI S3 호환 API 는 새 체크섬 헤더를 받지 않아 필요할 때만 쓰게 한다.
umask 077
cat > "$WORK/aws.env" << EOF
AWS_ACCESS_KEY_ID=$(env_value STORAGE_ACCESS_KEY)
AWS_SECRET_ACCESS_KEY=$(env_value STORAGE_SECRET_KEY)
AWS_DEFAULT_REGION=$REGION
AWS_REQUEST_CHECKSUM_CALCULATION=when_required
AWS_RESPONSE_CHECKSUM_VALIDATION=when_required
EOF
mkdir "$WORK/upload"
mv "$WORK"/peakda-*.dump "$WORK"/redis-*.rdb "$WORK/upload/"
docker run --rm \
  --env-file "$WORK/aws.env" \
  -v "$WORK/upload:/backup:ro" \
  amazon/aws-cli:2.27.0 \
  s3 cp /backup/ "s3://$BUCKET/$TS/" --recursive --endpoint-url "$ENDPOINT" --only-show-errors

log "완료: s3://$BUCKET/$TS/"
