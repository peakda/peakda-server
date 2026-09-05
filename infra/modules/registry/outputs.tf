output "repository_url" {
  description = "ECR 저장소 URL (docker push 대상)"
  value       = aws_ecr_repository.this.repository_url
}

output "repository_arn" {
  description = "ECR 저장소 ARN (IAM 정책용)"
  value       = aws_ecr_repository.this.arn
}

output "repository_name" {
  description = "ECR 저장소 이름"
  value       = aws_ecr_repository.this.name
}
