output "instance_id" {
  description = "GitHub Actions vars.EC2_INSTANCE_ID 에 설정할 값"
  value       = module.app_server.instance_id
}

output "public_ip" {
  description = "서버 고정 IP"
  value       = module.app_server.public_ip
}

output "app_url" {
  description = "서비스 URL"
  value       = "https://${local.app_domain}"
}

output "ecr_repository_url" {
  description = "ECR 저장소 URL"
  value       = module.registry.repository_url
}

output "github_deploy_role_arn" {
  description = "GitHub Actions vars.AWS_DEPLOY_ROLE_ARN 에 설정할 값"
  value       = module.github_oidc.role_arn
}

output "storage_bucket" {
  description = "백업·자산 버킷 (앱 이미지용이 아니다)"
  value       = module.backup.bucket_id
}

output "media_bucket" {
  description = "앱 이미지 버킷 (STORAGE_BUCKET)"
  value       = module.media.bucket_id
}

output "media_iam_user" {
  description = "액세스 키를 발급할 IAM 사용자. apply 후 CLI 로 키를 만들어 SSM 에 넣는다"
  value       = module.media.iam_user_name
}

output "route53_name_servers" {
  description = "도메인 등록기관에 입력할 네임서버. 존을 새로 만든 경우에만 값이 있다"
  value       = module.dns.name_servers
}

output "secrets_to_inject" {
  description = "apply 후 값을 주입해야 하는 SecureString 파라미터 목록"
  value       = module.config.secret_names
}

output "parameter_path" {
  description = "SSM 파라미터 경로"
  value       = module.config.path_prefix
}
