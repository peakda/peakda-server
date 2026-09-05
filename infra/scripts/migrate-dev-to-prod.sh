#!/usr/bin/env bash
# Safe, explicit dev -> prod PostgreSQL migration. The role flags are required
# because RDS hostnames do not contain a reliable environment name.
set -Eeuo pipefail

usage() {
  cat <<'EOF'
Usage:
  migrate-dev-to-prod.sh --source-url URL --target-url URL --source-role dev --target-role prod [--dry-run]
  migrate-dev-to-prod.sh --source-dump FILE --target-url URL --source-role dev --target-role prod --apply --confirm PROD_MIGRATION
  migrate-dev-to-prod.sh --source-url URL --target-url URL --source-role dev --target-role prod --apply --confirm PROD_MIGRATION

The target must already have the Liquibase schema. --apply replaces all target
application data (except Liquibase tables) after taking a target snapshot.
EOF
}

SOURCE_URL=''; SOURCE_DUMP=''; TARGET_URL=''; SOURCE_ROLE=''; TARGET_ROLE=''; MODE='dry-run'; CONFIRM=''
while (($#)); do
  case "$1" in
    --source-url) SOURCE_URL="${2:?missing source URL}"; shift 2 ;;
    --source-dump) SOURCE_DUMP="${2:?missing source dump}"; shift 2 ;;
    --target-url) TARGET_URL="${2:?missing target URL}"; shift 2 ;;
    --environment) TARGET_ROLE="${2:?missing environment}"; shift 2 ;;
    --source-role) SOURCE_ROLE="${2:?missing source role}"; shift 2 ;;
    --target-role) TARGET_ROLE="${2:?missing target role}"; shift 2 ;;
    --dry-run) MODE='dry-run'; shift ;;
    --apply) MODE='apply'; shift ;;
    --confirm) CONFIRM="${2:?missing confirmation}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "unknown option: $1" >&2; usage >&2; exit 2 ;;
  esac
done

[[ ( -n "$SOURCE_URL" || -n "$SOURCE_DUMP" ) && -n "$TARGET_URL" && "$SOURCE_ROLE" == 'dev' && "$TARGET_ROLE" == 'prod' ]] || { echo 'ERROR: source URL or --source-dump, target URL, and explicit dev/prod roles are required' >&2; exit 2; }
[[ -z "$SOURCE_DUMP" || -f "$SOURCE_DUMP" ]] || { echo "ERROR: source dump not found: $SOURCE_DUMP" >&2; exit 2; }
[[ -z "$SOURCE_URL" || "$SOURCE_URL" != "$TARGET_URL" ]] || { echo 'ERROR: source and target must differ' >&2; exit 2; }
# Do not infer environment from an RDS hostname. Require distinct, parseable
# hosts and explicit role flags instead; this also rejects accidental same-host
# migrations when both URLs use different credentials or query parameters.
connection_host() {
  local url="$1" rest authority
  if [[ "$url" == postgres://* || "$url" == postgresql://* ]]; then
    rest="${url#*://}"; authority="${rest%%/*}"; authority="${authority##*@}"
    printf '%s\n' "${authority%%:*}"
  else
    printf '%s\n' ""
  fi
}
TARGET_HOST="$(connection_host "$TARGET_URL")"
[[ -n "$TARGET_HOST" ]] || { echo 'ERROR: target host must be parseable' >&2; exit 2; }
if [[ -n "$SOURCE_URL" ]]; then
  SOURCE_HOST="$(connection_host "$SOURCE_URL")"
  [[ -n "$SOURCE_HOST" && "$SOURCE_HOST" != "$TARGET_HOST" ]] || { echo 'ERROR: source/target hosts must be parseable and different' >&2; exit 2; }
fi
if [[ "$MODE" == apply && "$CONFIRM" != 'PROD_MIGRATION' ]]; then echo 'ERROR: --apply requires --confirm PROD_MIGRATION' >&2; exit 2; fi
command -v pg_dump >/dev/null || { echo 'ERROR: pg_dump is required' >&2; exit 1; }
command -v pg_restore >/dev/null || { echo 'ERROR: pg_restore is required' >&2; exit 1; }
command -v psql >/dev/null || { echo 'ERROR: psql is required' >&2; exit 1; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
SANITIZE_SQL="$ROOT_DIR/infra/scripts/sanitize-migrated-data.sql"
if [[ -f "$SCRIPT_DIR/sanitize-migrated-data.sql" ]]; then
  SANITIZE_SQL="$SCRIPT_DIR/sanitize-migrated-data.sql"
fi
[[ -f "$SANITIZE_SQL" ]] || { echo "ERROR: sanitization SQL not found: $SANITIZE_SQL" >&2; exit 1; }
STAMP="$(date -u +%Y%m%d-%H%M%S)"
ARTIFACT_DIR="${MIGRATION_ARTIFACT_DIR:-$ROOT_DIR/.migration-artifacts/$STAMP}"
mkdir -p "$ARTIFACT_DIR"

if [[ -n "$SOURCE_DUMP" ]]; then
  cp "$SOURCE_DUMP" "$ARTIFACT_DIR/source.dump"
else
  echo "[migration] source backup: $ARTIFACT_DIR/source.dump"
  pg_dump --dbname="$SOURCE_URL" --format=custom --no-owner --no-acl \
    --exclude-table='public.databasechangelog*' \
    --exclude-table=public.scheduler_job_runs --exclude-table=public.signup_sessions \
    --file="$ARTIFACT_DIR/source.dump"
fi
pg_restore --list "$ARTIFACT_DIR/source.dump" > "$ARTIFACT_DIR/source.dump.list"
if grep -Eq 'TABLE DATA.*(databasechangelog|scheduler_job_runs|signup_sessions)' "$ARTIFACT_DIR/source.dump.list"; then
  echo 'ERROR: excluded table data is present in dump' >&2
  exit 1
fi
psql --dbname="$TARGET_URL" --set=ON_ERROR_STOP=1 -Atc "SELECT to_regclass('public.databasechangelog') IS NOT NULL AND to_regclass('public.databasechangeloglock') IS NOT NULL" | grep -qx t || { echo 'ERROR: target is not Liquibase-initialized' >&2; exit 1; }

if [[ "$MODE" == dry-run ]]; then
  echo '[migration] DRY RUN: source is unchanged and target is untouched; showing sanitized counts only'
  [[ -n "$SOURCE_URL" ]] || { echo 'ERROR: --dry-run requires --source-url (cannot inventory a dump without a database)' >&2; exit 2; }
  psql --dbname="$SOURCE_URL" --set=ON_ERROR_STOP=1 <<SQL
BEGIN;
\i '$SANITIZE_SQL'
ROLLBACK;
SQL
  exit 0
fi

echo "[migration] target pre-restore snapshot: $ARTIFACT_DIR/target-before.dump"
pg_dump --dbname="$TARGET_URL" --format=custom --no-owner --no-acl --file="$ARTIFACT_DIR/target-before.dump"
echo '[migration] clearing target application data (Liquibase tables preserved)'
psql --dbname="$TARGET_URL" --set=ON_ERROR_STOP=1 -c "SELECT format('TRUNCATE TABLE %I RESTART IDENTITY CASCADE;', tablename) FROM pg_tables WHERE schemaname='public' AND tablename NOT IN ('databasechangelog','databasechangeloglock','spatial_ref_sys')" --tuples-only --no-align | psql --dbname="$TARGET_URL" --set=ON_ERROR_STOP=1
# PostgreSQL 18 emits SET transaction_timeout in archive output, while the
# production PostgreSQL 16 server does not know that setting. Stream the
# data-only SQL and remove only that version-specific preamble statement.
pg_restore --data-only --no-owner --no-acl --file=- "$ARTIFACT_DIR/source.dump" \
  | sed '/^SET transaction_timeout = 0;$/d' \
  | psql --dbname="$TARGET_URL" --set=ON_ERROR_STOP=1
psql --dbname="$TARGET_URL" --set=ON_ERROR_STOP=1 > "$ARTIFACT_DIR/sanitize.log" <<SQL
BEGIN;
\i '$SANITIZE_SQL'
COMMIT;
SQL
psql --dbname="$TARGET_URL" --set=ON_ERROR_STOP=1 <<'SQL'
DO $$
DECLARE r record; max_id bigint;
BEGIN
  FOR r IN SELECT c.table_schema, c.table_name, c.column_name,
                   pg_get_serial_sequence(format('%I.%I', c.table_schema, c.table_name), c.column_name) AS seq
             FROM information_schema.columns c
             JOIN information_schema.tables t USING (table_schema, table_name)
            WHERE c.table_schema = 'public' AND t.table_type = 'BASE TABLE'
  LOOP
    IF r.seq IS NOT NULL THEN
      EXECUTE format('SELECT max(%I) FROM %I.%I', r.column_name, r.table_schema, r.table_name) INTO max_id;
      IF max_id IS NULL THEN
        PERFORM setval(r.seq, 1, false);
      ELSE
        PERFORM setval(r.seq, max_id, true);
      END IF;
    END IF;
  END LOOP;
END $$;
SQL
echo "[migration] complete; artifacts: $ARTIFACT_DIR"
