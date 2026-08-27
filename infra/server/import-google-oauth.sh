#!/usr/bin/env bash
# 다운로드한 Google OAuth 클라이언트 JSON을 SSM Parameter Store에 주입한다.
#
# 사용법:
#   ./import-google-oauth.sh <client-secret.json> [parameter-prefix] [aws-region]
#
# 기본값은 개발 서버(/peakda/dev, ap-northeast-2)이며, deploy.sh가 읽는
# PARAMETER_PREFIX/AWS_REGION 환경변수로도 덮어쓸 수 있다.
set -euo pipefail

if [ "$#" -lt 1 ] || [ "$#" -gt 3 ]; then
  echo "사용법: $0 <client-secret.json> [parameter-prefix] [aws-region]" >&2
  exit 2
fi

JSON_FILE="$1"
PARAMETER_PREFIX="${2:-${PARAMETER_PREFIX:-/peakda/dev}}"
AWS_REGION_VALUE="${3:-${AWS_REGION:-ap-northeast-2}}"

if [ ! -f "$JSON_FILE" ]; then
  echo "ERROR: JSON 파일을 찾을 수 없습니다: $JSON_FILE" >&2
  exit 1
fi

command -v jq >/dev/null 2>&1 || {
  echo "ERROR: jq가 필요합니다." >&2
  exit 1
}
command -v aws >/dev/null 2>&1 || {
  echo "ERROR: AWS CLI가 필요합니다." >&2
  exit 1
}

CLIENT_ID="$(jq -er '.web.client_id // .installed.client_id' "$JSON_FILE")"
CLIENT_SECRET="$(jq -er '.web.client_secret // .installed.client_secret' "$JSON_FILE")"

put_secret() {
  local name="$1"
  local value="$2"

  aws ssm put-parameter \
    --name "$PARAMETER_PREFIX/$name" \
    --value "$value" \
    --type SecureString \
    --overwrite \
    --region "$AWS_REGION_VALUE" \
    --output text >/dev/null
}

put_secret GOOGLE_CLIENT_ID "$CLIENT_ID"
put_secret GOOGLE_CLIENT_SECRET "$CLIENT_SECRET"

echo "Google OAuth 환경변수를 SSM에 등록했습니다: $PARAMETER_PREFIX/{GOOGLE_CLIENT_ID,GOOGLE_CLIENT_SECRET}"
