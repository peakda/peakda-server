output "zone_id" {
  description = "호스팅 존 ID"
  value       = local.zone_id
}

output "name_servers" {
  description = "도메인 등록기관에 설정할 네임서버 목록 (존을 새로 만든 경우에만 값이 있다)"
  value       = var.create_zone ? aws_route53_zone.this[0].name_servers : []
}

output "fqdn" {
  description = "생성된 레코드 FQDN"
  value       = aws_route53_record.app.fqdn
}
