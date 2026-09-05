variable "name_prefix" {
  description = "리소스 이름 접두어 (예: peakda-dev)"
  type        = string
}

variable "vpc_cidr" {
  description = "VPC CIDR 블록"
  type        = string
  default     = "10.0.0.0/16"
}

variable "create_private_subnets" {
  description = "RDS/Redis 등 외부 라우트가 없는 private subnet 2개를 만들지 여부"
  type        = bool
  default     = false
}

variable "create_app_security_group" {
  description = "단일 EC2+Caddy 환경용 80/443 security group을 만들지 여부"
  type        = bool
  default     = true
}
