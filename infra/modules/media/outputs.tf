output "bucket_id" {
  description = "이미지 버킷 이름 (STORAGE_BUCKET)"
  value       = aws_s3_bucket.media.id
}

output "bucket_arn" {
  description = "이미지 버킷 ARN"
  value       = aws_s3_bucket.media.arn
}

output "iam_user_name" {
  description = "액세스 키를 발급할 IAM 사용자 이름"
  value       = aws_iam_user.media.name
}

output "bucket_regional_domain_name" {
  description = "CloudFront origin 으로 쓸 버킷 리전 도메인"
  value       = aws_s3_bucket.media.bucket_regional_domain_name
}
