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
