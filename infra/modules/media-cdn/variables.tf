variable "name_prefix" {
  description = "리소스 이름 접두사 (예: peakda-prod)"
  type        = string
}

variable "bucket_id" {
  description = "이미지 버킷 이름"
  type        = string
}

variable "bucket_arn" {
  description = "이미지 버킷 ARN"
  type        = string
}

variable "bucket_regional_domain_name" {
  description = "이미지 버킷의 리전 도메인 (CloudFront origin)"
  type        = string
}

variable "domain_name" {
  description = "CDN 도메인 (예: cdn.peakda.com)"
  type        = string
}

variable "route53_zone_id" {
  description = "도메인이 속한 Route53 호스팅 존 id"
  type        = string
}

variable "price_class" {
  description = "CloudFront 엣지 범위. 한국 사용자를 받으려면 최소 PriceClass_200"
  type        = string
  default     = "PriceClass_200"
}
