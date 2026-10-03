variable "region" {
  description = "AWS 리전"
  type        = string
  default     = "ap-northeast-2"
}

variable "env" {
  description = "환경 이름. SSM 파라미터 경로와 Spring 프로파일에 함께 쓰인다"
  type        = string
  default     = "dev"
}

# ---------------------------------------------------------------------------
# 네트워크 / 컴퓨트
# ---------------------------------------------------------------------------

variable "vpc_cidr" {
  description = "VPC CIDR"
  type        = string
  default     = "10.0.0.0/16"
}

variable "instance_type" {
  description = "EC2 인스턴스 타입. 2GB 가 부족하면 t4g.medium 으로 올린다"
  type        = string
  default     = "t4g.small"
}

variable "root_volume_size" {
  description = "root 볼륨(GB). 도커 이미지가 쌓인다"
  type        = number
  default     = 20
}

variable "data_volume_size" {
  description = "데이터 볼륨(GB). PostgreSQL·Redis 데이터"
  type        = number
  default     = 10
}

# ---------------------------------------------------------------------------
# 도메인
# ---------------------------------------------------------------------------

variable "domain_name" {
  description = "루트 도메인"
  type        = string
  default     = "peakda.com"
}

variable "subdomain" {
  description = "서비스 서브도메인. 최종 FQDN 은 <subdomain>.<domain_name>"
  type        = string
  default     = "api-dev"
}

variable "cdn_subdomain" {
  description = "이미지 CDN 서브도메인. 최종 FQDN 은 <cdn_subdomain>.<domain_name>"
  type        = string
  default     = "cdn-dev"
}

variable "create_dns_zone" {
  description = "Route53 호스팅 존을 이 환경에서 생성할지 여부. 존은 환경 간 공유한다"
  type        = bool
  default     = true
}

# ---------------------------------------------------------------------------
# 레지스트리 / CI
# ---------------------------------------------------------------------------

variable "ecr_repository_name" {
  description = "ECR 저장소 이름"
  type        = string
  default     = "peakda-server"
}

variable "create_oidc_provider" {
  description = "GitHub OIDC provider 생성 여부. 계정당 1개만 존재할 수 있다"
  type        = bool
  default     = true
}

variable "github_allowed_subjects" {
  description = "배포 역할을 assume 할 수 있는 GitHub OIDC subject 목록"
  type        = list(string)
}

# ---------------------------------------------------------------------------
# 애플리케이션 설정
# ---------------------------------------------------------------------------

variable "app_parameters" {
  description = "SSM 에 등록할 일반 파라미터. main.tf 에서 산출되는 값과 병합된다"
  type        = map(string)
  default     = {}
}

variable "app_secret_names" {
  description = "SecureString 으로 만들 파라미터 이름. 값은 CLI 로 주입한다"
  type        = list(string)
}

# ---------------------------------------------------------------------------
# 비용
# ---------------------------------------------------------------------------

variable "monthly_budget_limit" {
  description = "월 예산 한도(USD). 80% 실사용·100% 예측 시 알림"
  type        = string
  default     = "30"
}

variable "alert_email" {
  description = "예산 알림 수신 이메일"
  type        = string
}

# ---------------------------------------------------------------------------
# prod 연결 (개발자 로컬 접근 경로)
#
# 로컬 → dev 앱 서버 → prod RDS 로 붙기 위해 VPC 피어링과 prod DB 보안그룹 허용이 필요하다.
# 피어링 연결(pcx-)은 콘솔에서 만든 것을 그대로 쓰고, 라우트만 Terraform 이 관리한다.
# 값을 비우면 경로를 만들지 않는다.
# ---------------------------------------------------------------------------

variable "prod_peering_connection_id" {
  description = "dev↔prod VPC 피어링 연결 id. 비우면 피어링 경로를 만들지 않는다"
  type        = string
  default     = "pcx-023bd0318dac1af64"
}

variable "prod_vpc_cidr" {
  description = "prod VPC CIDR. 피어링 경로의 목적지"
  type        = string
  default     = "10.20.0.0/16"
}

# 이미지 URL 을 CDN 고정 주소로 내릴지 여부.
# false 면 STORAGE_PUBLIC_BASE_URL 을 주지 않아 앱이 presigned URL 로 폴백한다.
# 프런트가 next/image 허용 목록에 CDN 도메인을 갖고 있어야 한다 — 없으면 이미지가 전부 깨진다.
variable "media_cdn_enabled" {
  description = "이미지 응답에 CDN 공개 주소를 쓸지 여부"
  type        = bool
  default     = true
}
