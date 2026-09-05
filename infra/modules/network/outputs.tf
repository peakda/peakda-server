output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.this.id
}

output "public_subnet_ids" {
  description = "public subnet ID 목록 (2 AZ)"
  value       = aws_subnet.public[*].id
}

output "public_subnet_azs" {
  description = "public subnet 의 AZ 목록. EBS 볼륨을 같은 AZ 에 만들 때 필요하다"
  value       = aws_subnet.public[*].availability_zone
}

output "app_security_group_id" {
  description = "앱 서버 보안 그룹 ID"
  value       = try(aws_security_group.app[0].id, null)
}

output "private_subnet_ids" {
  description = "Private subnet IDs for production data services"
  value       = aws_subnet.private[*].id
}
