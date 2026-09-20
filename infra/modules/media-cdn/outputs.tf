output "public_base_url" {
  description = "앱에 넣을 STORAGE_PUBLIC_BASE_URL 값"
  value       = "https://${var.domain_name}"
}

output "distribution_id" {
  description = "CloudFront 배포 id"
  value       = aws_cloudfront_distribution.this.id
}

output "distribution_domain_name" {
  description = "CloudFront 기본 도메인 (alias 레코드가 가리키는 대상)"
  value       = aws_cloudfront_distribution.this.domain_name
}
