#!/usr/bin/env bash
# PostgreSQL·Redis 데이터를 다른 AWS 계정·환경으로 무손실 이관한다.
#
# 원본과 대상이 서로 다른 계정·VPC 에 있어 한 곳에서 둘 다 닿기 어렵다는 전제로 단계를 나눈다.
#   export  원본에 닿는 곳에서 덤프·지문·Redis 키를 디렉터리(와 선택적으로 S3)로 내보낸다
#   import  대상에 닿는 곳에서 그 산출물을 받아 복원하고 원본 지문과 대조한다
#   verify  원본 또는 대상의 지금 상태를 export 시점 지문과 다시 대조한다
#           (--side source: export 뒤 원본에 쓰기가 있었는지, --side target: import 뒤 대상에 쓰기가 있었는지)
#
# 사용법
#   migrate-data.sh export --dir DIR [--s3 s3://bucket/prefix] [--skip-redis]
#   migrate-data.sh import --dir DIR [--s3 s3://bucket/prefix] [--skip-redis] [--reset-target]
#   migrate-data.sh verify --dir DIR [--s3 s3://bucket/prefix] [--side source|target]
#   전체 절차: infra/scripts/data-migration.md
#
# 접속 정보는 명령행이 아니라 환경변수로 받는다. 비밀번호가 프로세스 목록에 남지 않게 하기 위함이다.
#   SOURCE_PGHOST  SOURCE_PGUSER  SOURCE_PGPASSWORD
#   SOURCE_PGPORT(5432)  SOURCE_PGDATABASE(peakda)  SOURCE_PGSSLMODE(require)
#   TARGET_PG*     위와 같다
#   SOURCE_REDIS_URL / TARGET_REDIS_URL   redis://[[user]:password@]host[:port][/db] 또는 rediss://(TLS)
#                                         비밀번호는 URL 인코딩하지 않은 값이어야 한다
#   SOURCE_REDIS_CACERT / TARGET_REDIS_CACERT     TLS CA 파일. 없으면 시스템 CA(ElastiCache 는 이것으로 충분)
#   SOURCE_REDIS_INSECURE / TARGET_REDIS_INSECURE 1 이면 TLS 인증서 검증을 생략한다
#   REDIS_KEY_PATTERNS   옮길 키 패턴. 기본값은 "refresh:* quota:*"
#
# PostgreSQL 클라이언트 버전은 원본 서버 이상, 대상 서버 이하여야 한다. 이미지를 원본 서버의
# 메이저 버전으로 빌드하면(Dockerfile.data-migration 의 PG_MAJOR) 둘 다 만족한다.
#
# 유실을 막는 장치
#   - 원본은 읽기만 한다
#   - 원본·대상에 다른 세션이 있으면 시작하지 않는다. 앱을 내린 뒤 실행한다
#   - 대상 DB·Redis 가 비어 있지 않으면 시작하지 않는다. --reset-target 은 대상 DB 를 덤프해 둔 뒤 비운다
#   - 복원은 단일 트랜잭션이라 오류가 하나라도 나면 전부 되돌려진다
#   - 테이블별 행 수·행 체크섬·시퀀스 값을 원본 지문과 대조하고, 하나라도 다르면 실패한다
#   - Redis 문자열 키를 만료 절대 시각째 옮기고 값을 바이트 단위로 대조한다
#   - 산출물은 SHA256SUMS 로 전송 중 손상을 검사한다
#
# Redis 는 문자열 키만 옮긴다. refresh:* 는 리프레시 토큰, quota:* 는 외부 API 일일 쿼터 카운터다.
# oauth2:*·auth:code:* 는 수 분짜리 로그인 중간 상태이고 스케줄러 락은 옮기면 안 되므로 기본값에서 뺐다.
set -Eeuo pipefail

REDIS_KEY_PATTERNS="${REDIS_KEY_PATTERNS:-refresh:* quota:*}"

log() { echo "[migrate-data] $(date -u +%FT%TZ) $*" >&2; }
die() {
  log "ERROR: $*"
  exit 1
}

usage() { sed -n '2,17p' "$0" | sed 's/^# \{0,1\}//'; }

# ---------------------------------------------------------------------------
# 접속
# ---------------------------------------------------------------------------

# pg SOURCE|TARGET <command...> : 해당 쪽 접속 정보를 libpq 환경변수로 넘겨 실행한다.
pg() {
  local side="$1"
  shift
  local host="${side}_PGHOST" port="${side}_PGPORT" user="${side}_PGUSER" password="${side}_PGPASSWORD"
  local database="${side}_PGDATABASE" sslmode="${side}_PGSSLMODE"
  PGHOST="${!host:?${side}_PGHOST 가 필요하다}" \
    PGPORT="${!port:-5432}" \
    PGUSER="${!user:?${side}_PGUSER 가 필요하다}" \
    PGPASSWORD="${!password:?${side}_PGPASSWORD 가 필요하다}" \
    PGDATABASE="${!database:-peakda}" \
    PGSSLMODE="${!sslmode:-require}" \
    PGTZ=UTC \
    PGAPPNAME=peakda-data-migration \
    "$@"
}

psql_on() {
  local side="$1"
  shift
  pg "$side" psql -X -q -v ON_ERROR_STOP=1 "$@"
}

# rcli SOURCE|TARGET <redis-cli args...>
rcli() {
  local side="$1"
  shift
  local url_var="${side}_REDIS_URL" cacert_var="${side}_REDIS_CACERT" insecure_var="${side}_REDIS_INSECURE"
  local url="${!url_var:?${side}_REDIS_URL 이 필요하다. Redis 를 옮기지 않으려면 --skip-redis}"
  local tls=0 rest auth="" user="" password="" hostport path host port db=0

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
  path=""
  [[ "$rest" == */* ]] && path="${rest#*/}"
  [[ -n "$path" ]] && db="$path"
  host="${hostport%%:*}"
  port=6379
  [[ "$hostport" == *:* ]] && port="${hostport##*:}"

  local args=(-h "$host" -p "$port" -n "$db")
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
# 점검
# ---------------------------------------------------------------------------

client_major() { pg_dump --version | sed -E 's/.* ([0-9]+)(\.[0-9]+)*.*/\1/'; }
server_major() { psql_on "$1" -tAc "SELECT current_setting('server_version_num')::int / 10000"; }

# 이관 중 다른 세션이 쓰면 덤프·지문·복원 결과가 어긋난다. 앱이 내려가 있어야 한다.
require_no_sessions() {
  local side="$1" where count
  where="FROM pg_stat_activity WHERE datname = current_database() AND pid <> pg_backend_pid() AND backend_type = 'client backend'"
  count="$(psql_on "$side" -tAc "SELECT count(*) $where")"
  if [[ "$count" != 0 ]]; then
    psql_on "$side" -c "SELECT usename, application_name, client_addr, state, backend_start $where"
    die "$side DB 에 다른 세션이 ${count}개 있다. 앱과 접속 도구를 모두 내린 뒤 다시 실행한다"
  fi
}

# 테이블마다 "스키마.테이블|행 수|행 체크섬", 시퀀스마다 "sequence 이름|last_value|-" 를 출력한다.
# 행 체크섬은 행 텍스트 md5 를 정렬해 이어 붙인 md5 라 물리적 저장 순서와 무관하다.
# timestamptz 표현이 세션 시간대를 따르므로 PGTZ=UTC 로 접속하고(pg 함수), 정렬은 COLLATE "C" 로
# 고정해 양쪽 기본 collation 차이에 흔들리지 않게 한다.
fingerprint() {
  psql_on "$1" -tA -F'|' <<'SQL'
SELECT format(
  'SELECT %L, count(*), coalesce(md5(string_agg(md5(t::text), %L ORDER BY md5(t::text) COLLATE "C")), %L) FROM %I.%I t',
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
  rows="$(awk -F'|' '$1 !~ /^sequence / { s += $2 } END { print s + 0 }' "$actual")"
  log "$label: 원본 지문과 일치 (테이블 ${tables}개, 행 ${rows}개, 체크섬·시퀀스 포함)"
}

# 키 이름은 SCAN 출력(한 줄에 하나)과 redis-cli 인자 파싱을 거치므로 안전한 문자만 허용한다.
SAFE_KEY='^[A-Za-z0-9:_./@-]+$'

redis_now_ms() {
  local side="$1" time seconds micros
  time="$(rcli "$side" TIME)"
  { read -r seconds; read -r micros; } <<< "$time"
  [[ "$seconds" =~ ^[0-9]+$ && "$micros" =~ ^[0-9]+$ ]] || die "$side Redis 시각을 읽지 못했다: $time"
  echo $((seconds * 1000 + micros / 1000))
}

redis_pattern_count() {
  local side="$1" pattern total=0 n
  set -f
  for pattern in $REDIS_KEY_PATTERNS; do
    n="$(rcli "$side" --scan --pattern "$pattern" --count 1000 | grep -c . || true)"
    total=$((total + n))
  done
  set +f
  echo "$total"
}

# ---------------------------------------------------------------------------
# 산출물
# ---------------------------------------------------------------------------

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
  local dir="$1" key value
  [[ -f "$dir/SHA256SUMS" && -f "$dir/manifest.env" ]] || die "$dir 에 export 산출물이 없다"
  (cd "$dir" && sha256sum --quiet -c SHA256SUMS) || die "산출물이 손상됐다 (SHA256SUMS 불일치)"
  # source 하지 않고 정해진 키만 읽는다. 산출물이 바뀌어도 코드가 실행되지 않게 한다.
  while IFS='=' read -r key value; do
    value="${value#\'}"
    value="${value%\'}"
    case "$key" in
      EXPORTED_AT | SOURCE_SERVER_MAJOR | DUMP_CLIENT_MAJOR | SOURCE_EXTENSIONS | REDIS_EXPORTED | REDIS_KEY_COUNT)
        printf -v "$key" '%s' "$value"
        ;;
    esac
  done < "$dir/manifest.env"
  [[ "${SOURCE_SERVER_MAJOR:-}" =~ ^[0-9]+$ && "${DUMP_CLIENT_MAJOR:-}" =~ ^[0-9]+$ ]] || die "manifest.env 를 읽지 못했다"
  [[ "${SOURCE_EXTENSIONS:-}" =~ ^[a-z0-9_\ ]*$ ]] || die "manifest.env 의 확장 목록이 올바르지 않다"
}

# ---------------------------------------------------------------------------
# export
# ---------------------------------------------------------------------------

cmd_export() {
  local dir="$1" s3="$2" skip_redis="$3"
  prepare_empty_dir "$dir"

  local source_major client
  source_major="$(server_major SOURCE)"
  client="$(client_major)"
  ((client >= source_major)) || die "pg_dump ${client} 은 원본 서버 ${source_major} 보다 낮다. PG_MAJOR=${source_major} 로 이미지를 빌드한다"

  require_no_sessions SOURCE

  local extensions
  extensions="$(psql_on SOURCE -tAc "SELECT coalesce(string_agg(extname, ' ' ORDER BY extname), '') FROM pg_extension WHERE extname <> 'plpgsql'")"

  log "PostgreSQL 덤프 (원본 ${source_major}, 클라이언트 ${client})"
  pg SOURCE pg_dump -Fc --no-owner --no-acl -f "$dir/postgres.dump"
  log "덤프 완료: $(du -h "$dir/postgres.dump" | cut -f1)"

  log "원본 지문 계산"
  fingerprint SOURCE > "$dir/postgres.fingerprint"

  local redis_keys=0
  if [[ "$skip_redis" == 1 ]]; then
    log "Redis 는 건너뛴다 (--skip-redis)"
  else
    log "Redis 키 내보내기: $REDIS_KEY_PATTERNS"
    [[ "$(rcli SOURCE PING)" == PONG ]] || die "원본 Redis 에 접속하지 못했다"
    : > "$dir/redis.tsv"
    local pattern key type expire value
    set -f
    for pattern in $REDIS_KEY_PATTERNS; do
      # 파이프·프로세스 치환 안의 실패는 set -e 에 걸리지 않으므로 파일로 받은 뒤 읽는다.
      rcli SOURCE --scan --pattern "$pattern" --count 1000 > "$dir/.redis-keys"
      while IFS= read -r key; do
        [[ -n "$key" ]] || continue
        [[ "$key" =~ $SAFE_KEY ]] || die "옮길 수 없는 문자가 든 키(또는 Redis 오류): $key"
        type="$(rcli SOURCE TYPE "$key")"
        [[ "$type" == none ]] && continue
        [[ "$type" == string ]] || die "문자열이 아닌 키는 옮기지 않는다: $key ($type). REDIS_KEY_PATTERNS 를 좁힌다"
        # 절대 만료 시각(ms). 파일을 옮기는 동안 흐른 시간만큼 TTL 이 늘어나지 않게 한다. -1 은 만료 없음.
        expire="$(rcli SOURCE PEXPIRETIME "$key")"
        [[ "$expire" =~ ^-?[0-9]+$ ]] || die "만료 시각을 읽지 못했다(Redis 7 이상 필요): $key -> $expire"
        [[ "$expire" == -2 ]] && continue
        # --csv 는 값을 따옴표로 감싸고 제어문자·비ASCII 를 이스케이프한다. import 때 redis-cli 가
        # 같은 규칙으로 되읽으므로 바이트 그대로 옮겨진다.
        value="$(rcli SOURCE --csv GET "$key")"
        [[ "$value" == \"*\" ]] || die "값을 읽지 못했다: $key"
        printf '%s\t%s\t%s\n' "$expire" "$key" "$value" >> "$dir/redis.tsv"
        redis_keys=$((redis_keys + 1))
      done < "$dir/.redis-keys"
    done
    set +f
    rm -f "$dir/.redis-keys"
    log "Redis 키 ${redis_keys}개"
  fi

  # 덤프·지문 사이에 쓰기가 끼어들었는지 한 번 더 본다.
  require_no_sessions SOURCE

  cat > "$dir/manifest.env" <<EOF
EXPORTED_AT='$(date -u +%FT%TZ)'
SOURCE_SERVER_MAJOR='$source_major'
DUMP_CLIENT_MAJOR='$client'
SOURCE_EXTENSIONS='$extensions'
REDIS_EXPORTED='$([[ "$skip_redis" == 1 ]] && echo 0 || echo 1)'
REDIS_KEY_COUNT='$redis_keys'
EOF
  (cd "$dir" && sha256sum -- * | grep -v ' SHA256SUMS$' > SHA256SUMS)

  if [[ -n "$s3" ]]; then
    log "S3 업로드: $s3"
    aws s3 cp "$dir/" "${s3%/}/" --recursive --sse AES256 --only-show-errors
  fi
  log "export 완료. 이 시점부터 원본에 쓰기가 생기면 안 된다(verify --side source 로 확인)"
}

# ---------------------------------------------------------------------------
# import
# ---------------------------------------------------------------------------

cmd_import() {
  local dir="$1" s3="$2" skip_redis="$3" reset="$4"
  [[ -n "$s3" ]] && fetch_from_s3 "$dir" "$s3"
  check_manifest "$dir"

  local target_major client
  target_major="$(server_major TARGET)"
  client="$(client_major)"
  ((target_major >= SOURCE_SERVER_MAJOR)) || die "대상 서버 ${target_major} 가 원본 ${SOURCE_SERVER_MAJOR} 보다 낮다"
  ((client >= DUMP_CLIENT_MAJOR)) || die "pg_restore ${client} 로는 pg_dump ${DUMP_CLIENT_MAJOR} 덤프를 읽을 수 없다"
  ((client <= target_major)) || die "pg_restore ${client} 가 대상 서버 ${target_major} 보다 높다. PG_MAJOR=${SOURCE_SERVER_MAJOR} 이미지로 실행한다"

  local do_redis=1
  if [[ "$skip_redis" == 1 ]]; then
    do_redis=0
  elif [[ "$REDIS_EXPORTED" != 1 ]]; then
    die "export 가 Redis 를 건너뛰었다. 의도한 것이면 --skip-redis 를 붙인다"
  fi

  require_no_sessions TARGET

  local ext
  for ext in $SOURCE_EXTENSIONS; do
    [[ "$(psql_on TARGET -tAc "SELECT count(*) FROM pg_available_extensions WHERE name = '$ext'")" == 1 ]] \
      || die "대상 서버에 없는 확장이 원본에 있다: $ext"
  done

  local tables keys=0 database="${TARGET_PGDATABASE:-peakda}"
  tables="$(psql_on TARGET -tAc "SELECT count(*) FROM pg_tables WHERE schemaname NOT IN ('pg_catalog', 'information_schema')")"
  if ((do_redis)); then
    [[ "$(rcli TARGET PING)" == PONG ]] || die "대상 Redis 에 접속하지 못했다"
    keys="$(redis_pattern_count TARGET)"
  fi
  if [[ "$tables" != 0 || "$keys" != 0 ]]; then
    [[ "$reset" == 1 ]] || die "대상이 비어 있지 않다 (테이블 ${tables}개, Redis 키 ${keys}개). 대상 앱이 먼저 떴다면 --reset-target 으로 비운다"
    [[ "$database" =~ ^[a-z_][a-z0-9_]*$ ]] || die "DB 이름을 확인할 수 없다: $database"
    log "대상 초기화 (--reset-target): 기존 대상 DB 를 $dir/target-before-reset.dump 에 남긴다"
    pg TARGET pg_dump -Fc -f "$dir/target-before-reset.dump"
    psql_on TARGET -d postgres -c "DROP DATABASE \"$database\" WITH (FORCE)"
    psql_on TARGET -d postgres -c "CREATE DATABASE \"$database\""
    if ((do_redis)); then
      local pattern
      set -f
      for pattern in $REDIS_KEY_PATTERNS; do
        rcli TARGET --scan --pattern "$pattern" --count 1000 | while IFS= read -r key; do
          rcli TARGET DEL "$key" > /dev/null
        done
      done
      set +f
    fi
  fi

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

import_redis() {
  local dir="$1" now expire key value
  now="$(redis_now_ms TARGET)"
  local commands="$dir/redis.commands" expected="$dir/redis.expected" skipped=0
  : > "$commands"
  : > "$expected"
  while IFS=$'\t' read -r expire key value; do
    [[ -n "$key" ]] || continue
    if [[ "$expire" == -1 ]]; then
      printf 'SET %s %s\n' "$key" "$value" >> "$commands"
    elif ((expire > now)); then
      printf 'SET %s %s PXAT %s\n' "$key" "$value" "$expire" >> "$commands"
    else
      skipped=$((skipped + 1))
      continue
    fi
    # 대조 도중 만료될 키(1분 이내)는 대조에서 뺀다.
    if [[ "$expire" == -1 ]] || ((expire > now + 60000)); then
      printf '%s\t%s\n' "$key" "$value" >> "$expected"
    fi
  done < "$dir/redis.tsv"

  local sent ok
  sent="$(grep -c . "$commands" || true)"
  log "Redis 키 쓰기 ${sent}개 (그사이 만료 ${skipped}개)"
  if ((sent > 0)); then
    ok="$(rcli TARGET < "$commands" | grep -c '^OK$' || true)"
    [[ "$ok" == "$sent" ]] || die "Redis 쓰기 ${sent}개 중 ${ok}개만 성공했다"
  fi

  # 값은 --csv 표현(바이트 단위 이스케이프)끼리 비교한다.
  local mismatch=0 checked=0 actual
  while IFS=$'\t' read -r key value; do
    actual="$(rcli TARGET --csv GET "$key")"
    if [[ "$actual" == "$value" ]]; then
      checked=$((checked + 1))
    else
      log "값 불일치: $key"
      mismatch=$((mismatch + 1))
    fi
  done < "$expected"
  ((mismatch == 0)) || die "Redis 값 불일치 ${mismatch}개"
  log "Redis: 원본과 일치 (대조 ${checked}개)"
}

# ---------------------------------------------------------------------------
# verify
# ---------------------------------------------------------------------------

cmd_verify() {
  local dir="$1" s3="$2" side="$3"
  [[ -n "$s3" ]] && fetch_from_s3 "$dir" "$s3"
  check_manifest "$dir"
  local upper actual
  case "$side" in
    source) upper=SOURCE ;;
    target) upper=TARGET ;;
    *) die "--side 는 source 또는 target 이다" ;;
  esac
  actual="$dir/verify-$side-$(date -u +%Y%m%d-%H%M%S).fingerprint"
  fingerprint "$upper" > "$actual"
  compare_fingerprint "$dir/postgres.fingerprint" "$actual" "$side PostgreSQL"
}

# ---------------------------------------------------------------------------

main() {
  local cmd="${1:-}"
  case "$cmd" in
    "") usage; exit 2 ;;
    -h | --help) usage; exit 0 ;;
  esac
  shift
  local dir="" s3="" skip_redis=0 reset=0 side=target
  while (($#)); do
    case "$1" in
      --dir) dir="${2:?--dir 값이 필요하다}"; shift 2 ;;
      --s3) s3="${2:?--s3 값이 필요하다}"; shift 2 ;;
      --skip-redis) skip_redis=1; shift ;;
      --reset-target) reset=1; shift ;;
      --side) side="${2:?--side 값이 필요하다}"; shift 2 ;;
      -h | --help) usage; exit 0 ;;
      *) die "알 수 없는 옵션: $1" ;;
    esac
  done
  [[ -n "$dir" ]] || die "--dir 이 필요하다"

  # 산출물에는 개인정보가 든 덤프가 있다. 다른 사용자가 읽지 못하게 한다.
  umask 077

  case "$cmd" in
    export) cmd_export "$dir" "$s3" "$skip_redis" ;;
    import) cmd_import "$dir" "$s3" "$skip_redis" "$reset" ;;
    verify) cmd_verify "$dir" "$s3" "$side" ;;
    *) die "알 수 없는 명령: $cmd (export | import | verify)" ;;
  esac
}

main "$@"
