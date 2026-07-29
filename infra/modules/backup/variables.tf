variable "bucket_name" {
  description = "백업·서버자산 버킷 이름 (전역 유일해야 한다)"
  type        = string
}

variable "backup_retention_days" {
  description = "postgres/ 프리픽스 덤프 보존 일수"
  type        = number
  default     = 30
}
