output "path_prefix" {
  description = "파라미터 경로 프리픽스 (deploy.sh 가 이 경로를 조회한다)"
  value       = local.prefix
}

output "secret_names" {
  description = "값 주입이 필요한 SecureString 파라미터 이름"
  value       = var.secret_names
}
