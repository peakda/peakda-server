#!/bin/bash
# PostgreSQL 백업. cron 이 매일 03:00 KST(18:00 UTC)에 실행한다.
#
# S3 lifecycle 이 30일 뒤 삭제하므로 여기서 원격 보관본을 지우지 않는다.
set -euo pipefail

APP_DIR=/opt/peakda
cd "$APP_DIR"
# shellcheck source=/dev/null
source "$APP_DIR/env.conf"

log() { echo "[backup] $(date -Is) $*"; }

TS="$(date -u +%Y%m%d-%H%M%S)"
FILE="peakda-$TS.dump"
TMP="/tmp/$FILE"

# -Fc 는 custom format 으로 자체 압축된다. gzip 을 덧씌우지 않는다.
log "pg_dump 시작"
docker compose exec -T postgres pg_dump -U peakda -Fc --no-owner --no-acl peakda > "$TMP"

SIZE="$(du -h "$TMP" | cut -f1)"
log "덤프 완료: $SIZE"

aws s3 cp "$TMP" "s3://$ASSETS_BUCKET/postgres/$FILE" --region "$AWS_REGION" --quiet
log "업로드 완료: s3://$ASSETS_BUCKET/postgres/$FILE"

rm -f "$TMP"

# ---------------------------------------------------------------------------
# 복구 절차 (리허설 필수)
#
#   aws s3 cp s3://<bucket>/postgres/<파일> /tmp/restore.dump
#   docker compose exec -T postgres pg_restore -U peakda -d peakda \
#     --clean --if-exists --no-owner --no-acl < /tmp/restore.dump
# ---------------------------------------------------------------------------
