#!/bin/bash
# S3 에 올려둔 덤프로 PostgreSQL 을 복원한다. SSM Send-Command 로 호출된다.
#
#   사용법: restore.sh <s3-key> [--wipe]
#
#   <s3-key>  버킷 안 경로. 예: migration/railway-20260729.dump
#   --wipe    복원 전 기존 데이터를 지운다. Liquibase 관리 테이블은 건드리지 않는다.
#
# 스키마는 Liquibase 가 이미 만들어 두었으므로 데이터만 넣는다.
set -euo pipefail

APP_DIR=/opt/peakda
cd "$APP_DIR"
# shellcheck source=/dev/null
source "$APP_DIR/env.conf"

S3_KEY="${1:?복원할 S3 key 를 지정하세요}"
WIPE="${2:-}"

log() { echo "[restore] $(date -Is) $*"; }
psql_run() { docker compose exec -T postgres psql -U peakda -d peakda "$@"; }

TMP="/tmp/restore-$(date +%s).dump"
trap 'rm -f "$TMP"' EXIT

# ---------------------------------------------------------------------------
# 1. 덤프 내려받기
# ---------------------------------------------------------------------------
log "다운로드: s3://$ASSETS_BUCKET/$S3_KEY"
aws s3 cp "s3://$ASSETS_BUCKET/$S3_KEY" "$TMP" --region "$AWS_REGION" --quiet
log "크기: $(du -h "$TMP" | cut -f1)"

# ---------------------------------------------------------------------------
# 2. 안전장치 — 복원 전 현재 상태를 백업해 둔다
# ---------------------------------------------------------------------------
PRE="/tmp/pre-restore-$(date -u +%Y%m%d-%H%M%S).dump"
log "복원 전 스냅샷 생성"
docker compose exec -T postgres pg_dump -U peakda -Fc --no-owner --no-acl peakda > "$PRE"
aws s3 cp "$PRE" "s3://$ASSETS_BUCKET/postgres/$(basename "$PRE")" --region "$AWS_REGION" --quiet
log "스냅샷 저장: s3://$ASSETS_BUCKET/postgres/$(basename "$PRE")"
rm -f "$PRE"

# ---------------------------------------------------------------------------
# 3. 기존 데이터 정리
#    databasechangelog·databasechangeloglock 은 제외한다.
#    지우면 Liquibase 가 changelog 를 다시 적용하려 들어 스키마가 깨진다.
# ---------------------------------------------------------------------------
if [ "$WIPE" = "--wipe" ]; then
  log "기존 데이터 삭제 (Liquibase 관리 테이블 제외)"
  psql_run -tAc "
    SELECT string_agg(format('%I', tablename), ', ')
    FROM pg_tables
    WHERE schemaname = 'public'
      AND tablename NOT IN ('databasechangelog', 'databasechangeloglock')
  " | while read -r tables; do
    [ -z "$tables" ] && continue
    psql_run -c "TRUNCATE TABLE $tables RESTART IDENTITY CASCADE;"
  done
  log "삭제 완료"
fi

# ---------------------------------------------------------------------------
# 4. 복원
#    --data-only  : 스키마는 Liquibase 소관이므로 건드리지 않는다
#    --disable-triggers : FK 검증 순서를 신경 쓰지 않고 넣는다(superuser 필요)
# ---------------------------------------------------------------------------
log "복원 시작"
set +e
docker compose exec -T postgres pg_restore \
  -U peakda -d peakda \
  --data-only \
  --disable-triggers \
  --no-owner --no-acl \
  --exclude-schema=none \
  < "$TMP" 2> /tmp/restore-err.log
RC=$?
set -e

if [ -s /tmp/restore-err.log ]; then
  # databasechangelog 중복 등 무시해도 되는 경고가 섞이므로 전문을 남긴다.
  log "pg_restore 메시지:"
  tail -30 /tmp/restore-err.log
fi

if [ $RC -ne 0 ]; then
  log "pg_restore 종료 코드 $RC — 위 메시지를 확인한다"
fi

# ---------------------------------------------------------------------------
# 5. 시퀀스 정합성
#    --data-only 복원은 setval 을 포함하지만, 누락된 경우를 대비해 재계산한다.
#    id 최대값보다 시퀀스가 뒤처지면 이후 INSERT 가 PK 충돌로 실패한다.
# ---------------------------------------------------------------------------
log "시퀀스 재설정"
psql_run -tAc "
  SELECT string_agg(
    format('SELECT setval(%L, COALESCE((SELECT MAX(%I) FROM %I), 1));',
           pg_get_serial_sequence(quote_ident(t.table_name), c.column_name),
           c.column_name, t.table_name),
    ' '
  )
  FROM information_schema.tables t
  JOIN information_schema.columns c
    ON c.table_name = t.table_name AND c.table_schema = t.table_schema
  WHERE t.table_schema = 'public'
    AND t.table_type = 'BASE TABLE'
    AND pg_get_serial_sequence(quote_ident(t.table_name), c.column_name) IS NOT NULL
" | while read -r stmts; do
  [ -z "$stmts" ] && continue
  psql_run -q -c "$stmts" > /dev/null
done

# ---------------------------------------------------------------------------
# 6. 결과
# ---------------------------------------------------------------------------
log "복원 후 행 수 (상위 25개)"
psql_run -c "
  SELECT relname AS table, n_live_tup AS rows
  FROM pg_stat_user_tables
  WHERE n_live_tup > 0
  ORDER BY n_live_tup DESC
  LIMIT 25
"

log "완료"
