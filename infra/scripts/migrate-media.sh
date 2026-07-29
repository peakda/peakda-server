#!/bin/bash
# Railway Bucket 의 이미지를 S3 로 옮긴다.
#
# rclone 없이 AWS CLI 만으로 처리한다(Railway Bucket 은 S3 호환이라
# --endpoint-url 로 접근할 수 있다). 로컬 디스크를 경유하므로
# 데이터가 수 GB 이상이면 rclone 을 쓰는 편이 빠르다.
#
#   사용법:
#     export RAILWAY_BUCKET_ENDPOINT='https://...'
#     export RAILWAY_BUCKET_NAME='...'
#     export RAILWAY_ACCESS_KEY='...'
#     export RAILWAY_SECRET_KEY='...'
#     ./migrate-media.sh
#
# 값은 Railway 대시보드 → 프로젝트 → Bucket → Variables 에서 확인한다.
set -euo pipefail

: "${RAILWAY_BUCKET_ENDPOINT:?RAILWAY_BUCKET_ENDPOINT 를 설정하세요}"
: "${RAILWAY_BUCKET_NAME:?RAILWAY_BUCKET_NAME 을 설정하세요}"
: "${RAILWAY_ACCESS_KEY:?RAILWAY_ACCESS_KEY 를 설정하세요}"
: "${RAILWAY_SECRET_KEY:?RAILWAY_SECRET_KEY 를 설정하세요}"

ENV="${1:-dev}"
REGION="${AWS_REGION:-ap-northeast-2}"
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

TARGET_BUCKET="$(aws ssm get-parameter --name "/peakda/$ENV/STORAGE_BUCKET" \
  --region "$REGION" --query Parameter.Value --output text)"

echo "원본: s3://$RAILWAY_BUCKET_NAME ($RAILWAY_BUCKET_ENDPOINT)"
echo "대상: s3://$TARGET_BUCKET"
echo "작업 디렉터리: $WORK_DIR"
echo

# ---------------------------------------------------------------------------
# 1. Railway Bucket → 로컬
#    자격증명을 이 단계에만 적용하기 위해 서브셸 안에서 export 한다.
# ---------------------------------------------------------------------------
echo "[1/3] 내려받는 중"
(
  export AWS_ACCESS_KEY_ID="$RAILWAY_ACCESS_KEY"
  export AWS_SECRET_ACCESS_KEY="$RAILWAY_SECRET_KEY"
  unset AWS_PROFILE AWS_SESSION_TOKEN
  aws s3 sync "s3://$RAILWAY_BUCKET_NAME" "$WORK_DIR" \
    --endpoint-url "$RAILWAY_BUCKET_ENDPOINT" \
    --no-progress
)

FILE_COUNT="$(find "$WORK_DIR" -type f | wc -l | tr -d ' ')"
TOTAL_SIZE="$(du -sh "$WORK_DIR" | cut -f1)"
echo "  받은 파일: $FILE_COUNT 개 ($TOTAL_SIZE)"

if [ "$FILE_COUNT" -eq 0 ]; then
  echo "옮길 파일이 없다. 종료한다."
  exit 0
fi

# ---------------------------------------------------------------------------
# 2. 로컬 → S3
#    앱 IAM 사용자가 아니라 현재 CLI 자격증명(관리자)으로 올린다.
# ---------------------------------------------------------------------------
echo "[2/3] 올리는 중"
aws s3 sync "$WORK_DIR" "s3://$TARGET_BUCKET" --region "$REGION" --no-progress

# ---------------------------------------------------------------------------
# 3. 검증
# ---------------------------------------------------------------------------
echo "[3/3] 검증"
UPLOADED="$(aws s3 ls "s3://$TARGET_BUCKET" --recursive --region "$REGION" | wc -l | tr -d ' ')"
echo "  원본 $FILE_COUNT 개 → 대상 $UPLOADED 개"

if [ "$UPLOADED" -lt "$FILE_COUNT" ]; then
  echo "  경고: 개수가 맞지 않는다. 다시 실행하거나 수동 확인이 필요하다." >&2
  exit 1
fi

echo "완료."
