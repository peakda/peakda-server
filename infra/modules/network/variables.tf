variable "name_prefix" {
  description = "리소스 이름 접두어 (예: peakda-dev)"
  type        = string
}

variable "vpc_cidr" {
  description = "VPC CIDR 블록"
  type        = string
  default     = "10.0.0.0/16"
}
