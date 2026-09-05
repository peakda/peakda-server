variable "name_prefix" {
  description = "리소스 이름 접두어"
  type        = string
}

variable "region" {
  description = "AWS 리전"
  type        = string
}

variable "account_id" {
  description = "AWS 계정 ID"
  type        = string
}

variable "create_oidc_provider" {
  description = "OIDC provider 를 새로 만들지 여부. 계정에 이미 있으면 false (계정당 1개만 존재 가능)"
  type        = bool
  default     = true
}

variable "thumbprint_list" {
  description = "GitHub OIDC 인증서 thumbprint. AWS 가 자동 검증하므로 값 자체는 형식만 맞으면 된다"
  type        = list(string)
  default     = ["6938fd4d98bab03faadb97b34396831e3780aea1"]
}

variable "allowed_subjects" {
  description = "AssumeRole 을 허용할 GitHub OIDC subject 목록"
  type        = list(string)
}

variable "ecr_repository_arn" {
  description = "push 를 허용할 ECR 저장소 ARN"
  type        = string
}

variable "target_instance_id" {
  description = "SSM 배포 명령을 보낼 EC2 인스턴스 ID"
  type        = string
}
