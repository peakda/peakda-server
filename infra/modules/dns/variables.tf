variable "domain_name" {
  description = "루트 도메인 (예: peakda.com)"
  type        = string
}

variable "create_zone" {
  description = "호스팅 존을 새로 만들지 여부. 존은 환경 간 공유되므로 하나의 환경에서만 true"
  type        = bool
  default     = false
}

variable "record_name" {
  description = "생성할 레코드 FQDN (예: api-dev.peakda.com)"
  type        = string
}

variable "target_ip" {
  description = "A 레코드가 가리킬 IP"
  type        = string
}

variable "record_ttl" {
  description = "레코드 TTL(초). 컷오버 시 빠른 전환을 위해 짧게 둔다"
  type        = number
  default     = 300
}
