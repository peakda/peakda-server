#!/bin/bash
# SSM Parameter Store 에 시크릿을 대화형으로 주입한다.
#
#   사용법: ./put-secrets.sh [env]        (기본 env=dev)
#
# 입력값은 화면에 표시되지 않고 셸 히스토리에도 남지 않는다.
# 값을 비우고 엔터를 치면 그 항목은 건너뛴다.
set -euo pipefail

ENV="${1:-dev}"
REGION="${AWS_REGION:-ap-northeast-2}"
PREFIX="/peakda/$ENV"
PLACEHOLDER="PLACEHOLDER_SET_ME_VIA_CLI"

# STORAGE_ENDPOINT·BUCKET·REGION·PATH_STYLE_ACCESS 는 여기 없다.
# S3 로 전환하면서 Terraform 이 관리하는 일반 파라미터가 되었다.
# STORAGE_ACCESS_KEY/SECRET_KEY 와 SPRING_DATASOURCE_PASSWORD 는 이미 주입되어 있어
# 기본적으로 건너뛰면 된다(키를 교체할 때만 다시 입력).
SECRETS=(
  JWT_SECRET
  KAKAO_CLIENT_ID
  KAKAO_CLIENT_SECRET
  NAVER_CLIENT_ID
  NAVER_CLIENT_SECRET
  KTO_SERVICE_KEY
  KMA_SERVICE_KEY
  PUBDATA_FESTIVAL_SERVICE_KEY
  STORAGE_ACCESS_KEY
  STORAGE_SECRET_KEY
  SPRING_DATASOURCE_PASSWORD
)

echo "대상: $PREFIX (리전 $REGION)"
echo "값은 화면에 보이지 않는다. 건너뛰려면 빈 값으로 엔터."
echo

for KEY in "${SECRETS[@]}"; do
  CURRENT="$(aws ssm get-parameter --name "$PREFIX/$KEY" --with-decryption \
    --region "$REGION" --query 'Parameter.Value' --output text 2>/dev/null || echo "")"

  if [ -z "$CURRENT" ]; then
    STATUS="없음"
  elif [ "$CURRENT" = "$PLACEHOLDER" ]; then
    STATUS="미주입"
  else
    STATUS="주입됨(${#CURRENT}자)"
  fi

  printf '%-32s [%s] : ' "$KEY" "$STATUS"
  read -rs VALUE
  echo

  if [ -z "$VALUE" ]; then
    continue
  fi

  aws ssm put-parameter \
    --name "$PREFIX/$KEY" \
    --value "$VALUE" \
    --type SecureString \
    --overwrite \
    --region "$REGION" \
    --query Version --output text > /dev/null

  echo "  → 저장 완료"
  unset VALUE
done

echo
echo "=== 남은 미주입 항목 ==="
REMAINING="$(aws ssm get-parameters-by-path --path "$PREFIX" --with-decryption --recursive \
  --region "$REGION" --query "Parameters[?Value=='$PLACEHOLDER'].Name" --output text)"

if [ -z "$REMAINING" ]; then
  echo "없음. 배포를 진행할 수 있다."
else
  echo "$REMAINING" | tr '\t' '\n' | sed "s|$PREFIX/|  - |"
fi
