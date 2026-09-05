variable "name_prefix" {
  description = "리소스 이름 접두어 (예: peakda-dev)"
  type        = string
}

variable "account_id" {
  description = "AWS 계정 ID"
  type        = string
}

variable "instance_type" {
  description = "EC2 인스턴스 타입. Graviton(arm64) 계열을 사용한다"
  type        = string
  default     = "t4g.small"
}

variable "subnet_id" {
  description = "인스턴스를 배치할 public subnet ID"
  type        = string
}

variable "availability_zone" {
  description = "인스턴스와 데이터 볼륨이 위치할 AZ (같아야 attach 가능)"
  type        = string
}

variable "security_group_id" {
  description = "보안 그룹 ID"
  type        = string
}

variable "root_volume_size" {
  description = "root 볼륨 크기(GB). 도커 이미지가 쌓이는 곳이다"
  type        = number
  default     = 20
}

variable "data_volume_size" {
  description = "데이터 볼륨 크기(GB). PostgreSQL·Redis 데이터가 저장된다"
  type        = number
  default     = 10
}

variable "ecr_repository_arn" {
  description = "ECR 저장소 ARN (pull 권한 범위)"
  type        = string
}

variable "ecr_repository_url" {
  description = "ECR 저장소 URL"
  type        = string
}

variable "parameter_path_prefix" {
  description = "SSM 파라미터 경로 프리픽스 (예: /peakda/dev)"
  type        = string
}

variable "backup_bucket_arn" {
  description = "백업·자산 버킷 ARN"
  type        = string
}

variable "backup_bucket_id" {
  description = "백업·자산 버킷 이름"
  type        = string
}
