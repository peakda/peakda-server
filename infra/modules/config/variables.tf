variable "env" {
  description = "환경 이름 (dev/prod). 파라미터 경로 /peakda/<env>/ 에 쓰인다"
  type        = string
}

variable "parameters" {
  description = "일반 파라미터. 값을 Terraform 이 관리한다"
  type        = map(string)
  default     = {}
}

variable "secret_names" {
  description = "SecureString 파라미터 이름 목록. 값은 CLI 로 주입한다"
  type        = list(string)
  default     = []
}
