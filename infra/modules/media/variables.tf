variable "bucket_name" {
  description = "이미지 버킷 이름 (전역 유일해야 한다)"
  type        = string
}

variable "iam_user_name" {
  description = "앱이 사용할 IAM 사용자 이름"
  type        = string
}
