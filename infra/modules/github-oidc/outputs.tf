output "role_arn" {
  description = "GitHub Actions 에 설정할 역할 ARN (vars.AWS_DEPLOY_ROLE_ARN)"
  value       = aws_iam_role.deploy.arn
}
