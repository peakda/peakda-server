env    = "dev"
region = "ap-northeast-2"

# ---------------------------------------------------------------------------
# 컴퓨트
# ---------------------------------------------------------------------------
instance_type    = "t4g.small" # 월 $15.18. 2GB 가 부족하면 t4g.medium
root_volume_size = 20
data_volume_size = 10

# ---------------------------------------------------------------------------
# 도메인
# ---------------------------------------------------------------------------
domain_name     = "peakda.com"
subdomain       = "api-dev"
create_dns_zone = true # 존은 dev 에서 만들고 prod 는 참조만 한다

# ---------------------------------------------------------------------------
# CI
# ---------------------------------------------------------------------------
create_oidc_provider = true

github_allowed_subjects = [
  "repo:peakda/peakda-server:ref:refs/heads/develop",
]

# ---------------------------------------------------------------------------
# 비용
# ---------------------------------------------------------------------------
monthly_budget_limit = "30"
alert_email          = "devjhyoo@gmail.com"

# ---------------------------------------------------------------------------
# 애플리케이션 설정 (일반 파라미터)
#
# TODO 표시된 값은 현재 Railway 설정과 프론트엔드 도메인이 확정되면 갱신한다.
# 갱신 후 terraform apply → 다음 배포에서 자동 반영된다.
# ---------------------------------------------------------------------------
app_parameters = {
  COOKIE_DOMAIN    = ".peakda.com"
  COOKIE_SECURE    = "true"
  COOKIE_SAME_SITE = "None"

  CORS_ALLOWED_ORIGINS = "https://peakda.vercel.app,https://peakda.com,https://www.peakda.com"
  OAUTH2_REDIRECT_URI  = "https://peakda.vercel.app/auth/callback"

  # Grafana Cloud 수집 엔드포인트. 시크릿이 아니라 계정 식별자라 여기에 값을 둔다.
  # 토큰만 SecureString 으로 분리한다.
  #
  # URL 은 push 경로까지 포함해야 한다. 호스트만 넣으면 Alloy 가 뜨기는 하고
  # 전송만 실패해서 "컨테이너는 정상인데 데이터가 안 온다" 로 나타난다.
  # 스택 리전은 도쿄(ap-northeast-0)로, 서울 EC2 에서 가장 가깝다.
  GRAFANA_CLOUD_PROM_URL  = "https://prometheus-prod-49-prod-ap-northeast-0.grafana.net/api/prom/push"
  GRAFANA_CLOUD_PROM_USER = "3408100"
  GRAFANA_CLOUD_LOKI_URL  = "https://logs-prod-030.grafana.net/loki/api/v1/push"
  GRAFANA_CLOUD_LOKI_USER = "1699725"

  # STORAGE_BUCKET·ENDPOINT·REGION·PATH_STYLE_ACCESS 는 main.tf 가 media 모듈에서
  # 산출한다. 여기에 다시 쓰면 그 값을 덮어쓰게 되므로 두지 않는다.
  #
  # 7일은 SigV4 presigned URL 의 최대값이다. 이 값을 쓰려면 앱 자격증명이
  # 장기 키여야 한다(임시 자격증명은 세션 만료 시 서명이 무효화된다).
  STORAGE_PRESIGNED_URL_TTL_SECONDS = "604800"

  SPOT_MATCHER_RADIUS_METERS = "50.0"
}

# ---------------------------------------------------------------------------
# 시크릿 (SecureString)
#
# 여기에는 이름만 선언한다. 값은 apply 후 CLI 로 1회 주입한다.
#   aws ssm put-parameter --name /peakda/dev/<KEY> --value '<값>' \
#     --type SecureString --overwrite --region ap-northeast-2
# ---------------------------------------------------------------------------
app_secret_names = [
  "SPRING_DATASOURCE_PASSWORD",
  "JWT_SECRET",
  "KAKAO_CLIENT_ID",
  "KAKAO_CLIENT_SECRET",
  "NAVER_CLIENT_ID",
  "NAVER_CLIENT_SECRET",
  "GOOGLE_CLIENT_ID",
  "GOOGLE_CLIENT_SECRET",
  "KTO_SERVICE_KEY",
  "KMA_SERVICE_KEY",
  "PUBDATA_FESTIVAL_SERVICE_KEY",
  "GRAFANA_CLOUD_API_KEY",

  # S3 IAM 사용자 액세스 키. apply 후 CLI 로 발급해 주입한다.
  # Terraform 으로 만들면 state 에 평문으로 남는다.
  "STORAGE_ACCESS_KEY",
  "STORAGE_SECRET_KEY",
]
