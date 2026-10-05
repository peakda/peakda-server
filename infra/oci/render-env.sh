#!/usr/bin/env bash
# AWS prod 의 SSM 설정을 그대로 읽어 OCI 서버용 .env 를 만든다. 로컬에서 한 번 실행한다.
#
#   사용법: infra/oci/render-env.sh <출력 파일(저장소 밖)> <앱 이미지>
#   예:     infra/oci/render-env.sh ~/peakda-oci.env ghcr.io/peakda/peakda-server:<sha>
#
# 필요한 환경변수 (OCI 값)
#   OCI_STORAGE_ACCESS_KEY / OCI_STORAGE_SECRET_KEY   Object Storage Customer Secret Key
#   ACME_EMAIL                                        Let's Encrypt 만료 알림 받을 주소
#
# JWT_SECRET·OAuth·FCM·외부 API 키는 값을 바꾸지 않고 옮긴다. JWT_SECRET 이 다르면 옮긴 리프레시
# 토큰이 전부 무효가 되고, OAuth 클라이언트가 다르면 기존 회원과 연결이 끊긴다.
# AWS 에 묶인 값(RDS·ElastiCache 주소, S3, JAVA_OPTS)만 OCI 서버 기준으로 바꾼다.
set -euo pipefail

OUT="${1:?출력 파일 경로가 필요하다(저장소 밖)}"
IMAGE="${2:?앱 이미지가 필요하다 (ghcr.io/peakda/peakda-server:<sha>)}"
: "${OCI_STORAGE_ACCESS_KEY:?OCI_STORAGE_ACCESS_KEY 가 필요하다}"
: "${OCI_STORAGE_SECRET_KEY:?OCI_STORAGE_SECRET_KEY 가 필요하다}"
: "${ACME_EMAIL:?ACME_EMAIL 이 필요하다}"

OCI_REGION=ap-osaka-1
OCI_NAMESPACE="$(oci os ns get --query data --raw-output)"
REPO_ROOT="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
case "$(cd "$(dirname "$OUT")" && pwd)/" in
  "$REPO_ROOT"/*) echo "출력 파일은 저장소 밖에 둔다(시크릿이 들어간다)" >&2; exit 1 ;;
esac

umask 077
tmp="$(mktemp)"
trap 'rm -f "$tmp"' EXIT

aws ssm get-parameters-by-path --path /peakda/prod --recursive --with-decryption --output json \
  | jq -r '.Parameters[] | "\(.Name | split("/") | last)=\(.Value)"' > "$tmp"
[[ "$(wc -l < "$tmp")" -ge 30 ]] || { echo "SSM 에서 읽은 항목이 너무 적다" >&2; exit 1; }

# compose 의 .env 는 따옴표 없는 값 안의 $ 와 " #" 를 해석한다. 값이 바뀌지 않도록 미리 막는다.
if grep -nE '\$|"|'"'"'| #' "$tmp" | cut -d= -f1 | grep .; then
  echo "위 항목 값에 compose 가 해석하는 문자가 있다. 그대로 옮기면 값이 바뀐다" >&2
  exit 1
fi

# OCI 서버 기준으로 바꾸거나 새로 넣는 항목. 같은 키가 SSM 에 있으면 이 값이 이긴다.
db_password="$(openssl rand -hex 24)"
overrides="$(cat << EOF
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/peakda
SPRING_DATASOURCE_USERNAME=peakda
SPRING_DATASOURCE_PASSWORD=$db_password
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=8
SPRING_DATA_REDIS_URL=redis://redis:6379
JAVA_OPTS=-Xmx1024m -XX:+ExitOnOutOfMemoryError
STORAGE_BUCKET=peakda-prod-media
STORAGE_ENDPOINT=https://$OCI_NAMESPACE.compat.objectstorage.$OCI_REGION.oraclecloud.com
STORAGE_REGION=$OCI_REGION
STORAGE_PATH_STYLE_ACCESS=true
STORAGE_ACCESS_KEY=$OCI_STORAGE_ACCESS_KEY
STORAGE_SECRET_KEY=$OCI_STORAGE_SECRET_KEY
STORAGE_PUBLIC_BASE_URL=https://cdn.peakda.com
CDN_DOMAIN=cdn.peakda.com
ACME_EMAIL=$ACME_EMAIL
OCI_REGION=$OCI_REGION
OCI_NAMESPACE=$OCI_NAMESPACE
BACKUP_BUCKET=peakda-prod-backup
APP_IMAGE=$IMAGE
EOF
)"

{
  echo "# peakda prod (OCI). $(date -u +%FT%TZ) 에 infra/oci/render-env.sh 로 생성. 권한 600 유지."
  pattern="^($(cut -d= -f1 <<< "$overrides" | paste -sd'|' -))="
  grep -vE "$pattern" "$tmp" | sort
  echo "$overrides"
} > "$OUT"
chmod 600 "$OUT"

# 같은 키가 두 번 있으면 compose 는 뒤의 값을 쓰지만, 사람이 읽을 때 헷갈리고 실수가 숨는다.
dupes="$(grep -v '^#' "$OUT" | cut -d= -f1 | sort | uniq -d)"
if [[ -n "$dupes" ]]; then
  rm -f "$OUT"
  echo "중복 키가 생겼다: $dupes" >&2
  exit 1
fi

echo "작성: $OUT ($(grep -c '=' "$OUT")개 항목). 서버로 옮긴 뒤 로컬 파일은 지운다:"
echo "  scp $OUT ubuntu@<서버>:/opt/peakda/.env && ssh ubuntu@<서버> chmod 600 /opt/peakda/.env && rm $OUT"
