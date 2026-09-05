output "instance_id" {
  description = "EC2 인스턴스 ID (SSM Send-Command 대상)"
  value       = aws_instance.this.id
}

output "public_ip" {
  description = "고정 공인 IP (Route53 A 레코드 대상)"
  value       = aws_eip.this.public_ip
}

output "role_arn" {
  description = "인스턴스 IAM 역할 ARN"
  value       = aws_iam_role.this.arn
}
