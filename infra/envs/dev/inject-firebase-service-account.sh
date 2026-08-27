#!/usr/bin/env bash
# Firebase Admin SDK 서비스 계정 JSON을 dev SSM SecureString에 주입한다.
# JSON 파일은 서버나 저장소로 복사하지 않고 base64 한 줄로만 저장한다.
set -euo pipefail

JSON_FILE="${1:-}"
AWS_REGION="${AWS_REGION:-ap-northeast-2}"
SSM_PARAMETER="${SSM_PARAMETER:-/peakda/dev/FCM_SERVICE_ACCOUNT_BASE64}"
SSM_PREFIX="${SSM_PARAMETER%/*}"

if [[ ! -f "$JSON_FILE" || ! -r "$JSON_FILE" ]]; then
  echo "사용법: $0 /path/to/firebase-service-account.json" >&2
  echo "서비스 계정 JSON을 읽을 수 없습니다: ${JSON_FILE:-<경로 없음>}" >&2
  exit 1
fi

command -v aws >/dev/null || { echo "aws CLI가 필요합니다." >&2; exit 1; }
command -v jq >/dev/null || { echo "jq가 필요합니다." >&2; exit 1; }

jq -e '(.type == "service_account") and (.project_id | length > 0) and (.private_key | length > 0)' \
  "$JSON_FILE" >/dev/null

PROJECT_ID="$(jq -r '.project_id' "$JSON_FILE")"
if [[ "$PROJECT_ID" != "peakda" ]]; then
  echo "예상하지 못한 Firebase project_id입니다: $PROJECT_ID" >&2
  exit 1
fi

ENCODED="$(base64 < "$JSON_FILE" | tr -d '\n')"
aws ssm put-parameter \
  --name "$SSM_PARAMETER" \
  --value "$ENCODED" \
  --type SecureString \
  --overwrite \
  --region "$AWS_REGION" \
  --no-cli-pager \
  >/dev/null

aws ssm put-parameter \
  --name "$SSM_PREFIX/FCM_ENABLED" \
  --value "true" \
  --type String \
  --overwrite \
  --region "$AWS_REGION" \
  --no-cli-pager \
  >/dev/null

aws ssm put-parameter \
  --name "$SSM_PREFIX/FCM_PROJECT_ID" \
  --value "$PROJECT_ID" \
  --type String \
  --overwrite \
  --region "$AWS_REGION" \
  --no-cli-pager \
  >/dev/null

echo "Firebase 서비스 계정 키를 SSM에 저장했습니다: $SSM_PARAMETER"
echo "프로젝트: $PROJECT_ID"
