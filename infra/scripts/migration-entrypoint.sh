#!/usr/bin/env bash
set -Eeuo pipefail

: "${PGHOST:?PGHOST is required}"
: "${PGUSER:?PGUSER is required}"
: "${PGPASSWORD:?PGPASSWORD is required}"

PGPORT="${PGPORT:-5432}"
PGDATABASE="${PGDATABASE:-peakda}"
TARGET_URL="postgresql://${PGUSER}@${PGHOST}:${PGPORT}/${PGDATABASE}?sslmode=require"
RUN_ID="${MIGRATION_RUN_ID:-$(date -u +%Y%m%d-%H%M%S)}"
WORK_DIR="/work/$RUN_ID"
ARTIFACT_DIR="$WORK_DIR/artifacts"
SOURCE_DUMP="$WORK_DIR/source.dump"

if [[ -n "${DATABASE_JOB_S3_URI:-}" ]]; then
  : "${DATABASE_JOB_SHA256:?DATABASE_JOB_SHA256 is required}"
  : "${DATABASE_JOB_MODE:?DATABASE_JOB_MODE is required (READ_ONLY or APPLY)}"
  SQL_FILE="$WORK_DIR/database-job.sql"
  mkdir -p "$WORK_DIR"
  echo "[database-job] downloading verified SQL"
  aws s3 cp "$DATABASE_JOB_S3_URI" "$SQL_FILE" --only-show-errors
  printf '%s  %s\n' "$DATABASE_JOB_SHA256" "$SQL_FILE" | sha256sum -c -
  case "$DATABASE_JOB_MODE" in
    READ_ONLY)
      echo '[database-job] executing in read-only mode'
      PGOPTIONS='-c default_transaction_read_only=on' \
        psql --dbname="$TARGET_URL" --set=ON_ERROR_STOP=1 --single-transaction --file="$SQL_FILE"
      ;;
    APPLY)
      [[ "${DATABASE_JOB_CONFIRM:-}" == 'PROD_DATABASE_APPLY' ]] || {
        echo 'ERROR: APPLY requires DATABASE_JOB_CONFIRM=PROD_DATABASE_APPLY' >&2
        exit 2
      }
      echo '[database-job] executing in apply mode'
      psql --dbname="$TARGET_URL" --set=ON_ERROR_STOP=1 --single-transaction --file="$SQL_FILE"
      ;;
    *)
      echo "ERROR: unsupported DATABASE_JOB_MODE: $DATABASE_JOB_MODE" >&2
      exit 2
      ;;
  esac
  echo '[database-job] complete'
  exit 0
fi

: "${MIGRATION_SOURCE_S3_URI:?MIGRATION_SOURCE_S3_URI is required}"
: "${MIGRATION_ARTIFACT_S3_PREFIX:?MIGRATION_ARTIFACT_S3_PREFIX is required}"
mkdir -p "$ARTIFACT_DIR"

upload_artifacts() {
  local exit_code=$?
  set +e
  aws s3 sync "$ARTIFACT_DIR/" "${MIGRATION_ARTIFACT_S3_PREFIX%/}/$RUN_ID/" --sse AES256 --only-show-errors
  local upload_code=$?
  if [[ $upload_code -ne 0 ]]; then
    echo "ERROR: migration artifacts upload failed with exit code $upload_code" >&2
    [[ $exit_code -ne 0 ]] || exit_code=$upload_code
  fi
  exit "$exit_code"
}
trap upload_artifacts EXIT

echo "[migration-entrypoint] downloading source dump"
aws s3 cp "$MIGRATION_SOURCE_S3_URI" "$SOURCE_DUMP" --only-show-errors

export MIGRATION_ARTIFACT_DIR="$ARTIFACT_DIR"

/opt/migration/migrate-dev-to-prod.sh \
  --source-dump "$SOURCE_DUMP" \
  --target-url "$TARGET_URL" \
  --source-role dev \
  --target-role prod \
  --apply \
  --confirm PROD_MIGRATION
