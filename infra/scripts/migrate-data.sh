#!/usr/bin/env bash
# PostgreSQL·Redis 데이터를 다른 AWS 계정·환경으로 무손실 이관한다.
#
# 원본과 대상이 서로 다른 계정·VPC 에 있어 한 곳에서 둘 다 닿기 어렵다는 전제로 단계를 나눈다.
#   preflight  다운타임 전에 한쪽의 접속·버전·권한·Redis 기능을 점검한다
#   export     원본에 닿는 곳에서 덤프·지문·Redis 키를 디렉터리(와 선택적으로 S3)로 내보낸다
#   import     대상에 닿는 곳에서 산출물을 받아 복원하고 원본 지문과 대조한다
#   verify     원본 또는 대상의 지금 상태(PostgreSQL·Redis)를 export 시점과 다시 대조한다
#
# 사용법
#   migrate-data.sh preflight --side source|target [--skip-redis]
#   migrate-data.sh export --dir DIR [--s3 s3://bucket/prefix] [--skip-redis] [--allow-empty]
#   migrate-data.sh import --dir DIR [--s3 ...] [--expect-sums SHA256] [--skip-redis] [--reset-target]
#   migrate-data.sh verify --dir DIR [--s3 ...] --side source|target [--skip-redis]
#   전체 절차: infra/scripts/data-migration.md
#
# 접속 정보는 명령행이 아니라 환경변수로 받는다. 비밀번호가 프로세스 목록에 남지 않게 하기 위함이다.
#   SOURCE_PGHOST  SOURCE_PGUSER  SOURCE_PGPASSWORD
#   SOURCE_PGPORT(5432)  SOURCE_PGDATABASE(peakda)  SOURCE_PGSSLMODE(require)
#   SOURCE_PGSSLROOTCERT   서버 인증서 CA. 이미지에 RDS 번들이 있다(/opt/migration/rds-global-bundle.pem)
#   TARGET_PG*             위와 같다
#   SOURCE_REDIS_URL / TARGET_REDIS_URL   redis://[[user]:password@]host[:port][/db] 또는 rediss://(TLS)
#                                         비밀번호는 URL 인코딩하지 않은 값이어야 한다
#   SOURCE_REDIS_CACERT / TARGET_REDIS_CACERT     TLS CA 파일. 없으면 시스템 CA(ElastiCache 는 이것으로 충분)
#   SOURCE_REDIS_INSECURE / TARGET_REDIS_INSECURE 1 이면 TLS 인증서 검증을 생략한다
#   REDIS_KEY_PATTERNS   옮길 키 패턴. 기본값은 "refresh:* quota:*"
#
# PostgreSQL 클라이언트 버전은 원본 서버 이상, 대상 서버 이하여야 한다. 이미지를 대상 서버의
# 메이저 버전으로 빌드하면(Dockerfile.data-migration 의 PG_MAJOR) 둘 다 만족한다.
#
# 유실을 막는 장치
#   - 원본은 읽기만 한다
#   - 원본·대상에 다른 세션이 있으면 시작하지 않는다. 앱을 내린 뒤 실행한다
#   - 원본이 비어 있으면(엉뚱한 DB·Redis 를 가리킨 경우) 시작하지 않는다
#   - 대상 DB·Redis 가 비어 있지 않으면 시작하지 않는다. --reset-target 은 대상 DB 를 덤프해 둔 뒤 비운다
#   - 복원은 단일 트랜잭션이라 오류가 하나라도 나면 전부 되돌려진다
#   - 테이블별 행 수·행 체크섬·시퀀스 값을 원본 지문과 대조하고, 하나라도 다르면 실패한다
#   - Redis 문자열 키를 만료 절대 시각째 옮기고, 값·만료 시각을 바이트 단위로 대조한다
#   - 산출물은 SHA256SUMS(--strict)·필수 파일·키 수로 검사하고, 읽은 값을 다시 검증한다
#
# Redis 는 문자열 키만 옮긴다. refresh:* 는 리프레시 토큰, quota:* 는 외부 API 일일 쿼터 카운터다.
# oauth2:*·auth:code:* 는 수 분짜리 로그인 중간 상태이고 스케줄러 락은 옮기면 안 되므로 기본값에서 뺐다.
set -Eeuo pipefail

REDIS_KEY_PATTERNS="${REDIS_KEY_PATTERNS:-refresh:* quota:*}"

# 키 이름은 SCAN 출력(한 줄에 하나)과 redis-cli 인자 파싱을 거치므로 안전한 문자만 허용한다.
SAFE_KEY='^[A-Za-z0-9:_./@-]+$'
# redis-cli --csv 가 내는 문자열 표현. 따옴표로 감싸고 제어문자·비ASCII 는 \xHH 로 이스케이프한다.
CSV_VALUE='^"([^"\\]|\\.)*"$'
PROBE_KEY='__peakda_migration_probe__'

log() { echo "[migrate-data] $(date -u +%FT%TZ) $*" >&2; }
die() {
  log "ERROR: $*"
  exit 1
}

usage() { awk 'NR == 1 { next } /^# 접속 정보/ { exit } { sub(/^# ?/, ""); print }' "$0"; }

# ---------------------------------------------------------------------------
# 접속
# ---------------------------------------------------------------------------

# pg SOURCE|TARGET <command...> : 해당 쪽 접속 정보를 libpq 환경변수로 넘겨 실행한다.
pg() {
  local side="$1"
  shift
  local host="${side}_PGHOST" port="${side}_PGPORT" user="${side}_PGUSER" password="${side}_PGPASSWORD"
  local database="${side}_PGDATABASE" sslmode="${side}_PGSSLMODE" rootcert="${side}_PGSSLROOTCERT"
  local -a vars=(
    "PGHOST=${!host:?${side}_PGHOST 가 필요하다}"
    "PGPORT=${!port:-5432}"
    "PGUSER=${!user:?${side}_PGUSER 가 필요하다}"
    "PGPASSWORD=${!password:?${side}_PGPASSWORD 가 필요하다}"
    "PGDATABASE=${!database:-peakda}"
    "PGSSLMODE=${!sslmode:-require}"
    "PGTZ=UTC"
    "PGAPPNAME=peakda-data-migration"
  )
  [[ -n "${!rootcert:-}" ]] && vars+=("PGSSLROOTCERT=${!rootcert}")
  env "${vars[@]}" "$@"
}

psql_on() {
  local side="$1"
  shift
  pg "$side" psql -X -q -v ON_ERROR_STOP=1 "$@"
}

# rcli SOURCE|TARGET <redis-cli args...>. 명령이 없으면 stdin 의 명령을 한 줄씩 실행한다.
rcli() {
  local side="$1"
  shift
  local url_var="${side}_REDIS_URL" cacert_var="${side}_REDIS_CACERT" insecure_var="${side}_REDIS_INSECURE"
  local url="${!url_var:?${side}_REDIS_URL 이 필요하다. Redis 를 옮기지 않으려면 --skip-redis}"
  local tls=0 rest auth="" user="" password="" hostport host port db=0

  case "$url" in
    rediss://*) tls=1 rest="${url#rediss://}" ;;
    redis://*) rest="${url#redis://}" ;;
    *) die "${side}_REDIS_URL 은 redis:// 또는 rediss:// 로 시작해야 한다" ;;
  esac
  if [[ "$rest" == *@* ]]; then
    auth="${rest%@*}"
    rest="${rest##*@}"
    if [[ "$auth" == *:* ]]; then
      user="${auth%%:*}"
      password="${auth#*:}"
    else
      password="$auth"
    fi
  fi
  hostport="${rest%%/*}"
  [[ "$rest" == */?* ]] && db="${rest#*/}"
  host="${hostport%%:*}"
  port=6379
  [[ "$hostport" == *:* ]] && port="${hostport##*:}"

  local -a args=(-h "$host" -p "$port" -n "$db")
  [[ -n "$user" ]] && args+=(--user "$user")
  if [[ "$tls" == 1 ]]; then
    args+=(--tls --sni "$host")
    if [[ "${!insecure_var:-0}" == 1 ]]; then
      args+=(--insecure)
    elif [[ -n "${!cacert_var:-}" ]]; then
      args+=(--cacert "${!cacert_var}")
    fi
  fi
  if [[ -n "$password" ]]; then
    REDISCLI_AUTH="$password" redis-cli "${args[@]}" "$@"
  else
    redis-cli "${args[@]}" "$@"
  fi
}

# ---------------------------------------------------------------------------
# PostgreSQL 점검·지문
# ---------------------------------------------------------------------------

client_major() { pg_dump --version | sed -E 's/.* ([0-9]+)(\.[0-9]+)*.*/\1/'; }
server_major() { psql_on "$1" -tAc "SELECT current_setting('server_version_num')::int / 10000"; }
user_table_count() { psql_on "$1" -tAc "SELECT count(*) FROM pg_tables WHERE schemaname NOT IN ('pg_catalog', 'information_schema')"; }

SESSIONS_WHERE="FROM pg_stat_activity WHERE datname = current_database() AND pid <> pg_backend_pid() AND backend_type = 'client backend'"

# 이관 중 다른 세션이 쓰면 덤프·지문·복원 결과가 어긋난다. 앱이 내려가 있어야 한다.
require_no_sessions() {
  local side="$1" count
  count="$(psql_on "$side" -tAc "SELECT count(*) $SESSIONS_WHERE")"
  if [[ "$count" != 0 ]]; then
    psql_on "$side" -c "SELECT usename, application_name, client_addr, state, backend_start $SESSIONS_WHERE"
    die "$side DB 에 다른 세션이 ${count}개 있다. 앱과 접속 도구를 모두 내린 뒤 다시 실행한다"
  fi
}

# 테이블마다 "스키마.테이블|행 수|행 체크섬", 시퀀스마다 "sequence 이름|last_value|-" 를 출력한다.
# 행 체크섬은 행 텍스트 md5 를 정렬해 이어 붙인 md5 라 물리적 저장 순서와 무관하다.
# 행 별칭(_fp_row)은 컬럼 이름과 겹치지 않게 골랐다. 겹치면 행 대신 그 컬럼만 해시된다.
# 값의 텍스트 표현이 세션 설정을 따르므로 시간대(PGTZ)와 출력 형식(PGOPTIONS)을 고정하고,
# 정렬은 COLLATE "C" 로 고정해 양쪽 기본 collation 차이에 흔들리지 않게 한다.
fingerprint() {
  pg "$1" env PGOPTIONS='-c extra_float_digits=1 -c DateStyle=ISO,MDY -c IntervalStyle=postgres -c bytea_output=hex' \
    psql -X -q -v ON_ERROR_STOP=1 -tA -F'|' << 'SQL'
SELECT format(
  'SELECT %L, count(*), coalesce(md5(string_agg(md5(_fp_row::text), %L ORDER BY md5(_fp_row::text) COLLATE "C")), %L) FROM %I.%I _fp_row',
  n.nspname || '.' || c.relname, '', '-', n.nspname, c.relname)
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE c.relkind IN ('r', 'p')
  AND n.nspname NOT IN ('pg_catalog', 'information_schema')
  AND n.nspname NOT LIKE 'pg\_%'
ORDER BY n.nspname, c.relname
\gexec
SELECT 'sequence ' || schemaname || '.' || sequencename, coalesce(last_value::text, 'unused'), '-'
FROM pg_sequences
ORDER BY schemaname, sequencename;
SQL
}

compare_fingerprint() {
  local expected="$1" actual="$2" label="$3"
  if ! diff -u "$expected" "$actual" > "$actual.diff"; then
    head -60 "$actual.diff" >&2
    die "$label: 원본 지문과 다르다 (전체 차이: $actual.diff)"
  fi
  local tables rows
  tables="$(grep -vc '^sequence ' "$actual" || true)"
  rows="$(awk -F'|' '$1 !~ /^sequence / { s += $2 } END { printf "%d", s }' "$actual")"
  log "$label: 원본 지문과 일치 (테이블 ${tables}개, 행 ${rows}개, 체크섬·시퀀스 포함)"
}

# ---------------------------------------------------------------------------
# Redis
# ---------------------------------------------------------------------------

redis_check() {
  local side="$1" info
  [[ "$(rcli "$side" PING)" == PONG ]] || die "$side Redis 에 접속하지 못했다"
  [[ "$(rcli "$side" PEXPIRETIME "$PROBE_KEY")" == -2 ]] || die "$side Redis 가 PEXPIRETIME 을 지원하지 않는다(7.0 이상 필요)"
  info="$(rcli "$side" INFO cluster | tr -d '\r')"
  [[ "$info" != *cluster_enabled:1* ]] || die "$side Redis 가 cluster mode 다. cluster mode 가 꺼진 Redis 만 지원한다"
}

redis_now_ms() {
  local side="$1" time seconds micros
  time="$(rcli "$side" TIME)"
  { read -r seconds; read -r micros; } <<< "$time"
  [[ "$seconds" =~ ^[0-9]+$ && "$micros" =~ ^[0-9]+$ ]] || die "$side Redis 시각을 읽지 못했다: $time"
  echo $((seconds * 1000 + micros / 1000))
}

# 패턴에 맞는 키를 정렬·중복 제거해 파일로 남긴다. 오류 응답도 키 검증에서 걸러진다.
redis_scan() {
  local side="$1" out="$2" pattern key
  : > "$out.raw"
  set -f
  for pattern in $REDIS_KEY_PATTERNS; do
    rcli "$side" --scan --pattern "$pattern" --count 1000 >> "$out.raw"
  done
  set +f
  while IFS= read -r key; do
    [[ -z "$key" || "$key" =~ $SAFE_KEY ]] || die "$side Redis 에서 옮길 수 없는 키(또는 오류 응답): $key"
  done < "$out.raw"
  { grep -v '^$' "$out.raw" || true; } | sort -u > "$out"
  rm -f "$out.raw"
}

# 키 목록의 현재 상태를 "만료시각<TAB>키<TAB>csv값" 으로 남긴다. 사라진 키는 뺀다.
# 키마다 프로세스를 띄우지 않고 명령별로 한 세션에 몰아 보낸다(TLS·AUTH 왕복을 줄인다).
redis_snapshot() {
  local side="$1" keys="$2" out="$3" n f key type expire value
  : > "$out"
  n="$(wc -l < "$keys" | tr -d ' ')"
  ((n > 0)) || return 0
  sed 's/^/TYPE /' "$keys" | rcli "$side" > "$out.type"
  sed 's/^/PEXPIRETIME /' "$keys" | rcli "$side" > "$out.expire"
  sed 's/^/GET /' "$keys" | rcli "$side" --csv > "$out.value"
  for f in type expire value; do
    [[ "$(wc -l < "$out.$f" | tr -d ' ')" == "$n" ]] || die "$side Redis 응답 줄 수가 키 수와 다르다 ($f)"
  done
  paste "$keys" "$out.type" "$out.expire" "$out.value" > "$out.joined"
  while IFS=$'\t' read -r key type expire value; do
    case "$type" in
      string) ;;
      none) continue ;;
      *) die "$side Redis: 문자열이 아닌 키는 옮기지 않는다: $key ($type). REDIS_KEY_PATTERNS 를 좁힌다" ;;
    esac
    [[ "$expire" =~ ^-?[0-9]+$ ]] || die "$side Redis 만료 시각을 읽지 못했다: $key -> $expire"
    [[ "$expire" == -2 || "$value" == NULL ]] && continue # 그사이 만료
    [[ "$value" =~ $CSV_VALUE ]] || die "$side Redis 값을 읽지 못했다: $key"
    printf '%s\t%s\t%s\n' "$expire" "$key" "$value" >> "$out"
  done < "$out.joined"
  rm -f "$out.type" "$out.expire" "$out.value" "$out.joined"
}

# expected(export 시점)와 actual(지금)을 키·만료 시각·값으로 대조한다.
# expected 에 있고 actual 에 없는 키는 만료 시각이 지났을 때만 정상이다.
# strict 면 actual 에만 있는 키(export 뒤 새로 생긴 키)도 차이로 본다.
compare_redis() {
  local expected="$1" actual="$2" side="$3" label="$4" strict="$5"
  local now expire key value missing=0 changed=0 added=0 matched=0 expired=0
  local -A actual_value=() actual_expire=() seen=()
  now="$(redis_now_ms "$side")"
  while IFS=$'\t' read -r expire key value; do
    actual_value["$key"]="$value"
    actual_expire["$key"]="$expire"
  done < "$actual"
  while IFS=$'\t' read -r expire key value; do
    seen["$key"]=1
    if [[ -z "${actual_value[$key]+x}" ]]; then
      if [[ "$expire" != -1 ]] && ((expire <= now)); then
        expired=$((expired + 1))
      else
        log "$label: 사라진 키 $key"
        missing=$((missing + 1))
      fi
    elif [[ "${actual_value[$key]}" != "$value" || "${actual_expire[$key]}" != "$expire" ]]; then
      log "$label: 값 또는 만료 시각이 다른 키 $key"
      changed=$((changed + 1))
    else
      matched=$((matched + 1))
    fi
  done < "$expected"
  if [[ "$strict" == 1 ]]; then
    for key in "${!actual_value[@]}"; do
      if [[ -z "${seen[$key]+x}" ]]; then
        log "$label: export 뒤 생긴 키 $key"
        added=$((added + 1))
      fi
    done
  fi
  ((missing + changed + added == 0)) || die "$label: Redis 가 export 시점과 다르다 (사라짐 ${missing}, 달라짐 ${changed}, 새로 생김 ${added})"
  log "$label: Redis 일치 (일치 ${matched}개, 그사이 만료 ${expired}개)"
}

# ---------------------------------------------------------------------------
# 산출물
# ---------------------------------------------------------------------------

# 산출물을 믿지 않고 다시 검증한다. 특히 expire 는 bash 산술식에 들어가므로 숫자만 허용한다.
valid_redis_row() {
  [[ "$1" =~ ^(-1|[0-9]+)$ && "$2" =~ $SAFE_KEY && "$3" =~ $CSV_VALUE ]]
}

prepare_empty_dir() {
  local dir="$1"
  if [[ -e "$dir" ]] && [[ -n "$(ls -A "$dir")" ]]; then
    die "$dir 가 비어 있지 않다. 이전 실행과 섞이지 않도록 새 디렉터리를 지정한다"
  fi
  mkdir -p "$dir"
}

fetch_from_s3() {
  local dir="$1" s3="$2"
  prepare_empty_dir "$dir"
  log "S3 에서 내려받기: $s3"
  aws s3 cp "${s3%/}/" "$dir/" --recursive --only-show-errors
}

check_manifest() {
  local dir="$1" expect_sums="$2" key value file
  [[ -f "$dir/SHA256SUMS" && -f "$dir/manifest.env" ]] || die "$dir 에 export 산출물이 없다"
  if [[ -n "$expect_sums" ]]; then
    [[ "$(sha256sum "$dir/SHA256SUMS" | cut -d' ' -f1)" == "$expect_sums" ]] \
      || die "SHA256SUMS 가 export 때 출력된 값과 다르다"
  fi
  (cd "$dir" && sha256sum --quiet --strict -c SHA256SUMS) || die "산출물이 손상됐다 (SHA256SUMS 불일치)"

  # source 하지 않고 정해진 키만 읽는다. 산출물이 바뀌어도 코드가 실행되지 않게 한다.
  while IFS='=' read -r key value; do
    value="${value#\'}"
    value="${value%\'}"
    case "$key" in
      EXPORTED_AT | SOURCE_HOST | SOURCE_SERVER_MAJOR | DUMP_CLIENT_MAJOR | SOURCE_EXTENSIONS | SOURCE_TABLE_COUNT | REDIS_EXPORTED | REDIS_KEY_COUNT)
        printf -v "$key" '%s' "$value"
        ;;
    esac
  done < "$dir/manifest.env"
  [[ "${SOURCE_SERVER_MAJOR:-}" =~ ^[0-9]+$ && "${DUMP_CLIENT_MAJOR:-}" =~ ^[0-9]+$ ]] || die "manifest.env 를 읽지 못했다"
  [[ "${REDIS_EXPORTED:-}" =~ ^[01]$ && "${REDIS_KEY_COUNT:-}" =~ ^[0-9]+$ ]] || die "manifest.env 의 Redis 항목이 올바르지 않다"
  [[ "${SOURCE_EXTENSIONS:-}" =~ ^[a-z0-9_\ -]*$ ]] || die "manifest.env 의 확장 목록이 올바르지 않다"

  local -a required=(manifest.env postgres.dump postgres.fingerprint)
  [[ "$REDIS_EXPORTED" == 1 ]] && required+=(redis.tsv)
  for file in "${required[@]}"; do
    grep -q "  $file\$" "$dir/SHA256SUMS" || die "SHA256SUMS 에 $file 가 없다"
  done
  if [[ "$REDIS_EXPORTED" == 1 ]]; then
    [[ "$(wc -l < "$dir/redis.tsv" | tr -d ' ')" == "$REDIS_KEY_COUNT" ]] || die "redis.tsv 키 수가 manifest 와 다르다"
    # 대상에 무엇이든 쓰기 전에 전부 검증한다. 복원 뒤에 걸리면 대상이 반쯤 채워진 채 남는다.
    local expire
    while IFS=$'\t' read -r expire key value; do
      valid_redis_row "$expire" "$key" "$value" || die "redis.tsv 형식이 올바르지 않다: $key"
    done < "$dir/redis.tsv"
  fi
  log "산출물 확인: export ${EXPORTED_AT:-?}, 원본 ${SOURCE_HOST:-?} (PG ${SOURCE_SERVER_MAJOR}), 테이블 ${SOURCE_TABLE_COUNT:-?}개, Redis 키 ${REDIS_KEY_COUNT}개"
}

# ---------------------------------------------------------------------------
# preflight
# ---------------------------------------------------------------------------

cmd_preflight() {
  local side="$1" skip_redis="$2" upper major client
  case "$side" in
    source) upper=SOURCE ;;
    target) upper=TARGET ;;
    *) die "--side 는 source 또는 target 이다" ;;
  esac
  major="$(server_major "$upper")"
  client="$(client_major)"
  log "$side PostgreSQL: 서버 ${major}, 클라이언트 ${client}, 테이블 $(user_table_count "$upper")개, 다른 세션 $(psql_on "$upper" -tAc "SELECT count(*) $SESSIONS_WHERE")개"
  if [[ "$upper" == SOURCE ]]; then
    ((client >= major)) || die "pg_dump ${client} 은 원본 서버 ${major} 보다 낮다. PG_MAJOR 를 대상 서버 버전으로 빌드한다"
    log "원본 확장: $(psql_on SOURCE -tAc "SELECT coalesce(string_agg(extname, ' ' ORDER BY extname), '(없음)') FROM pg_extension WHERE extname <> 'plpgsql'")"
  else
    ((client <= major)) || die "pg_restore ${client} 가 대상 서버 ${major} 보다 높다. PG_MAJOR=${major} 로 이미지를 빌드한다"
    log "대상 권한: DB 생성 $(psql_on TARGET -tAc "SELECT rolcreatedb OR rolsuper FROM pg_roles WHERE rolname = current_user") (--reset-target 에 필요)"
  fi

  if [[ "$skip_redis" != 1 ]]; then
    redis_check "$upper"
    local keys
    keys="$(mktemp)"
    redis_scan "$upper" "$keys"
    log "$side Redis: 패턴 키 $(wc -l < "$keys" | tr -d ' ')개, 전체 $(rcli "$upper" DBSIZE)개"
    rm -f "$keys"
    if [[ "$upper" == TARGET ]]; then
      # 쓰기 권한과 SET ... PXAT 지원을 1분짜리 임시 키로 확인한다.
      local at
      at=$(($(redis_now_ms TARGET) + 60000))
      [[ "$(rcli TARGET SET "$PROBE_KEY" 1 PXAT "$at")" == OK && "$(rcli TARGET PEXPIRETIME "$PROBE_KEY")" == "$at" ]] \
        || die "대상 Redis 에 SET ... PXAT 로 쓰지 못했다"
      rcli TARGET DEL "$PROBE_KEY" > /dev/null
    fi
  fi
  log "$side preflight 통과"
}

# ---------------------------------------------------------------------------
# export
# ---------------------------------------------------------------------------

cmd_export() {
  local dir="$1" s3="$2" skip_redis="$3" allow_empty="$4"
  prepare_empty_dir "$dir"

  local source_major client tables extensions
  source_major="$(server_major SOURCE)"
  client="$(client_major)"
  ((client >= source_major)) || die "pg_dump ${client} 은 원본 서버 ${source_major} 보다 낮다. PG_MAJOR 를 대상 서버 버전으로 빌드한다"

  require_no_sessions SOURCE

  tables="$(user_table_count SOURCE)"
  if [[ "$tables" == 0 && "$allow_empty" != 1 ]]; then
    die "원본 DB 에 테이블이 없다. SOURCE_PGDATABASE 가 맞는지 확인한다(의도한 것이면 --allow-empty)"
  fi
  extensions="$(psql_on SOURCE -tAc "SELECT coalesce(string_agg(extname, ' ' ORDER BY extname), '') FROM pg_extension WHERE extname <> 'plpgsql'")"

  log "PostgreSQL 덤프 (원본 ${source_major}, 클라이언트 ${client}, 테이블 ${tables}개)"
  pg SOURCE pg_dump -Fc --no-owner --no-acl -f "$dir/postgres.dump"
  log "덤프 완료: $(du -h "$dir/postgres.dump" | cut -f1)"

  log "원본 지문 계산"
  fingerprint SOURCE > "$dir/postgres.fingerprint"

  local redis_keys=0
  if [[ "$skip_redis" == 1 ]]; then
    log "Redis 는 건너뛴다 (--skip-redis)"
  else
    log "Redis 키 내보내기: $REDIS_KEY_PATTERNS"
    redis_check SOURCE
    redis_scan SOURCE "$dir/.redis-keys"
    redis_snapshot SOURCE "$dir/.redis-keys" "$dir/redis.tsv"
    rm -f "$dir/.redis-keys"
    redis_keys="$(wc -l < "$dir/redis.tsv" | tr -d ' ')"
    if [[ "$redis_keys" == 0 && "$allow_empty" != 1 ]]; then
      die "원본 Redis 에 옮길 키가 없다. SOURCE_REDIS_URL·REDIS_KEY_PATTERNS 를 확인한다(의도한 것이면 --allow-empty)"
    fi
    log "Redis 키 ${redis_keys}개 (원본 전체 $(rcli SOURCE DBSIZE)개)"
  fi

  # 덤프·지문 사이에 쓰기가 끼어들었는지 한 번 더 본다.
  require_no_sessions SOURCE

  cat > "$dir/manifest.env" << EOF
EXPORTED_AT='$(date -u +%FT%TZ)'
SOURCE_HOST='${SOURCE_PGHOST}'
SOURCE_SERVER_MAJOR='$source_major'
DUMP_CLIENT_MAJOR='$client'
SOURCE_EXTENSIONS='$extensions'
SOURCE_TABLE_COUNT='$tables'
REDIS_EXPORTED='$([[ "$skip_redis" == 1 ]] && echo 0 || echo 1)'
REDIS_KEY_COUNT='$redis_keys'
EOF
  (cd "$dir" && sha256sum -- * | grep -v ' SHA256SUMS$' > SHA256SUMS)

  if [[ -n "$s3" ]]; then
    log "S3 업로드: $s3"
    aws s3 cp "$dir/" "${s3%/}/" --recursive --sse AES256 --only-show-errors
  fi
  log "export 완료. import 에 --expect-sums $(sha256sum "$dir/SHA256SUMS" | cut -d' ' -f1)"
  log "이 시점부터 원본에 쓰기가 생기면 안 된다(verify --side source 로 확인)"
}

# ---------------------------------------------------------------------------
# import
# ---------------------------------------------------------------------------

cmd_import() {
  local dir="$1" s3="$2" skip_redis="$3" reset="$4" expect_sums="$5"
  [[ -n "$s3" ]] && fetch_from_s3 "$dir" "$s3"
  check_manifest "$dir" "$expect_sums"

  [[ "${TARGET_PGHOST:-}" != "${SOURCE_HOST:-}" ]] || die "TARGET_PGHOST 가 원본 호스트와 같다"

  local target_major client
  target_major="$(server_major TARGET)"
  client="$(client_major)"
  ((target_major >= SOURCE_SERVER_MAJOR)) || die "대상 서버 ${target_major} 가 원본 ${SOURCE_SERVER_MAJOR} 보다 낮다"
  ((client >= DUMP_CLIENT_MAJOR)) || die "pg_restore ${client} 로는 pg_dump ${DUMP_CLIENT_MAJOR} 덤프를 읽을 수 없다. export 와 같은 이미지로 실행한다"
  ((client <= target_major)) || die "pg_restore ${client} 가 대상 서버 ${target_major} 보다 높다. PG_MAJOR=${target_major} 이미지로 export 부터 다시 한다"

  local do_redis=1
  if [[ "$skip_redis" == 1 ]]; then
    do_redis=0
    [[ "$REDIS_EXPORTED" == 1 ]] && log "WARN: export 한 Redis 키 ${REDIS_KEY_COUNT}개를 --skip-redis 로 옮기지 않는다"
  elif [[ "$REDIS_EXPORTED" != 1 ]]; then
    die "export 가 Redis 를 건너뛰었다. 의도한 것이면 --skip-redis 를 붙인다"
  fi

  require_no_sessions TARGET

  local ext
  for ext in $SOURCE_EXTENSIONS; do
    [[ "$(psql_on TARGET -tAc "SELECT count(*) FROM pg_available_extensions WHERE name = '$ext'")" == 1 ]] \
      || die "대상 서버에 없는 확장이 원본에 있다: $ext"
  done

  local tables keys=0 database="${TARGET_PGDATABASE:-peakda}" key_file="$dir/.target-keys"
  tables="$(user_table_count TARGET)"
  : > "$key_file"
  if ((do_redis)); then
    redis_check TARGET
    redis_scan TARGET "$key_file"
    keys="$(wc -l < "$key_file" | tr -d ' ')"
  fi
  if [[ "$tables" != 0 || "$keys" != 0 ]]; then
    [[ "$reset" == 1 ]] || die "대상이 비어 있지 않다 (테이블 ${tables}개, Redis 키 ${keys}개). 대상 앱이 먼저 떴다면 --reset-target 으로 비운다"
    reset_target "$dir" "$s3" "$database" "$do_redis" "$key_file"
  fi
  rm -f "$key_file"

  log "PostgreSQL 복원 (단일 트랜잭션, 대상 ${target_major})"
  pg TARGET pg_restore --no-owner --no-acl --single-transaction --exit-on-error -d "$database" "$dir/postgres.dump"
  psql_on TARGET -c "ANALYZE"

  log "대상 지문 대조"
  fingerprint TARGET > "$dir/target.fingerprint"
  compare_fingerprint "$dir/postgres.fingerprint" "$dir/target.fingerprint" "PostgreSQL"

  if ((do_redis)); then
    import_redis "$dir"
  fi

  require_no_sessions TARGET
  log "import 완료. 원본 쪽에서 verify --side source 로 export 이후 쓰기가 없었는지 확인한 뒤 트래픽을 넘긴다"
}

reset_target() {
  local dir="$1" s3="$2" database="$3" do_redis="$4" key_file="$5" backup
  [[ "$database" =~ ^[a-z_][a-z0-9_]*$ ]] || die "DB 이름을 확인할 수 없다: $database"
  backup="target-before-reset-$(date -u +%Y%m%d-%H%M%S).dump"
  log "대상 초기화 (--reset-target): 기존 대상 DB 를 $dir/$backup 에 남긴다"
  pg TARGET pg_dump -Fc -f "$dir/$backup"
  if [[ -n "$s3" ]]; then
    # 태스크·임시 호스트에서 돌면 로컬 백업이 함께 사라진다. 지우기 전에 S3 에 올려 둔다.
    aws s3 cp "$dir/$backup" "${s3%/}/$backup" --sse AES256 --only-show-errors
    log "백업 업로드: ${s3%/}/$backup"
  fi
  psql_on TARGET -d postgres -c "DROP DATABASE \"$database\" WITH (FORCE)"
  psql_on TARGET -d postgres -c "CREATE DATABASE \"$database\""
  if ((do_redis)) && [[ -s "$key_file" ]]; then
    sed 's/^/DEL /' "$key_file" | rcli TARGET > /dev/null
    redis_scan TARGET "$key_file"
    [[ ! -s "$key_file" ]] || die "대상 Redis 키를 지우지 못했다"
  fi
}

import_redis() {
  local dir="$1" now expire key value skipped=0
  local commands="$dir/redis.commands" expected="$dir/redis.expected" keys="$dir/redis.keys"
  now="$(redis_now_ms TARGET)"
  : > "$commands"
  : > "$expected"
  : > "$keys"
  while IFS=$'\t' read -r expire key value; do
    valid_redis_row "$expire" "$key" "$value" || die "redis.tsv 형식이 올바르지 않다: $key"
    if [[ "$expire" == -1 ]]; then
      printf 'SET %s %s\n' "$key" "$value" >> "$commands"
    elif ((expire > now)); then
      printf 'SET %s %s PXAT %s\n' "$key" "$value" "$expire" >> "$commands"
    else
      skipped=$((skipped + 1))
      continue
    fi
    printf '%s\t%s\t%s\n' "$expire" "$key" "$value" >> "$expected"
    printf '%s\n' "$key" >> "$keys"
  done < "$dir/redis.tsv"

  local sent ok
  sent="$(wc -l < "$commands" | tr -d ' ')"
  log "Redis 키 쓰기 ${sent}개 (export 뒤 만료 ${skipped}개)"
  if ((sent > 0)); then
    ok="$(rcli TARGET < "$commands" | grep -c '^OK$' || true)"
    [[ "$ok" == "$sent" ]] || die "Redis 쓰기 ${sent}개 중 ${ok}개만 성공했다"
  fi

  redis_snapshot TARGET "$keys" "$dir/redis.actual"
  compare_redis "$expected" "$dir/redis.actual" TARGET "Redis 되읽기" 0
}

# ---------------------------------------------------------------------------
# verify
# ---------------------------------------------------------------------------

cmd_verify() {
  local dir="$1" s3="$2" side="$3" skip_redis="$4" expect_sums="$5" upper stamp
  [[ -n "$s3" ]] && fetch_from_s3 "$dir" "$s3"
  check_manifest "$dir" "$expect_sums"
  case "$side" in
    source) upper=SOURCE ;;
    target) upper=TARGET ;;
    *) die "--side 는 source 또는 target 이다" ;;
  esac
  stamp="$(date -u +%Y%m%d-%H%M%S)"
  fingerprint "$upper" > "$dir/verify-$side-$stamp.fingerprint"
  compare_fingerprint "$dir/postgres.fingerprint" "$dir/verify-$side-$stamp.fingerprint" "$side PostgreSQL"

  if [[ "$skip_redis" != 1 && "$REDIS_EXPORTED" == 1 ]]; then
    redis_check "$upper"
    redis_scan "$upper" "$dir/.verify-keys"
    redis_snapshot "$upper" "$dir/.verify-keys" "$dir/verify-$side-$stamp.redis"
    rm -f "$dir/.verify-keys"
    compare_redis "$dir/redis.tsv" "$dir/verify-$side-$stamp.redis" "$upper" "$side" 1
  fi
}

# ---------------------------------------------------------------------------

main() {
  local cmd="${1:-}"
  case "$cmd" in
    "") usage; exit 2 ;;
    -h | --help) usage; exit 0 ;;
  esac
  shift
  local dir="" s3="" skip_redis=0 reset=0 side="" allow_empty=0 expect_sums=""
  while (($#)); do
    case "$1" in
      --dir) dir="${2:?--dir 값이 필요하다}"; shift 2 ;;
      --s3) s3="${2:?--s3 값이 필요하다}"; shift 2 ;;
      --side) side="${2:?--side 값이 필요하다}"; shift 2 ;;
      --expect-sums) expect_sums="${2:?--expect-sums 값이 필요하다}"; shift 2 ;;
      --skip-redis) skip_redis=1; shift ;;
      --reset-target) reset=1; shift ;;
      --allow-empty) allow_empty=1; shift ;;
      -h | --help) usage; exit 0 ;;
      *) die "알 수 없는 옵션: $1" ;;
    esac
  done

  # 산출물에는 개인정보가 든 덤프가 있다. 다른 사용자가 읽지 못하게 한다.
  umask 077

  case "$cmd" in
    preflight) cmd_preflight "$side" "$skip_redis" ;;
    export)
      [[ -n "$dir" ]] || die "--dir 이 필요하다"
      cmd_export "$dir" "$s3" "$skip_redis" "$allow_empty"
      ;;
    import)
      [[ -n "$dir" ]] || die "--dir 이 필요하다"
      cmd_import "$dir" "$s3" "$skip_redis" "$reset" "$expect_sums"
      ;;
    verify)
      [[ -n "$dir" ]] || die "--dir 이 필요하다"
      [[ -n "$side" ]] || die "--side source|target 이 필요하다"
      cmd_verify "$dir" "$s3" "$side" "$skip_redis" "$expect_sums"
      ;;
    *) die "알 수 없는 명령: $cmd (preflight | export | import | verify)" ;;
  esac
}

main "$@"
