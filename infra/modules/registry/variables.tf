variable "repository_name" {
  description = "ECR 저장소 이름"
  type        = string
}

variable "keep_image_count" {
  description = "보관할 최근 이미지 개수"
  type        = number
  default     = 5
}
