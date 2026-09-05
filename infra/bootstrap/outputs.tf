output "tfstate_bucket" {
  description = "envs/* 의 backend \"s3\" 에 지정할 버킷 이름"
  value       = aws_s3_bucket.tfstate.id
}
