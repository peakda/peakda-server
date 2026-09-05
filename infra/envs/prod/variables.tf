variable "region" {
  type    = string
  default = "ap-northeast-2"
}
variable "vpc_cidr" {
  type    = string
  default = "10.20.0.0/16"
}
variable "domain_name" {
  type    = string
  default = "peakda.com"
}
variable "subdomain" {
  type    = string
  default = "api"
}
variable "ecr_repository_name" {
  type    = string
  default = "peakda-server"
}
variable "image_tag" {
  type    = string
  default = "latest"
}
variable "migration_image_tag" {
  description = "Image tag used by the manual ECS migration task"
  type        = string
  default     = "latest"
}
variable "migration_source_s3_uri" {
  description = "Private S3 URI of the reviewed dev custom dump copied into the prod migration prefix"
  type        = string
  default     = "s3://replace-me/migration/source.dump"
}
variable "desired_count" {
  type    = number
  default = 0
}
variable "min_capacity" {
  type    = number
  default = 0
}
variable "max_capacity" {
  type    = number
  default = 8
}
variable "task_cpu" {
  type    = number
  default = 512
}
variable "task_memory" {
  type    = number
  default = 2048
}
variable "db_instance_class" {
  type    = string
  default = "db.t4g.micro"
}
variable "redis_node_type" {
  type    = string
  default = "cache.t4g.micro"
}
variable "alert_email" { type = string }
variable "github_allowed_subjects" { type = list(string) }
variable "monthly_budget_limit" {
  type    = string
  default = "100"
}
variable "backup_retention_days" {
  type    = number
  default = 30
}
variable "alb_requests_per_target" {
  description = "ALB requests per target per minute used by target tracking"
  type        = number
  default     = 2340
}
variable "cors_allowed_origins" {
  type    = string
  default = "https://peakda.com,https://www.peakda.com,https://peakda.vercel.app"
}
variable "oauth2_redirect_uri" {
  type    = string
  default = "https://peakda.vercel.app/auth/callback"
}
variable "fcm_project_id" {
  type    = string
  default = "peakda"
}
variable "create_oidc_provider" {
  type    = bool
  default = false
}
variable "app_parameters" {
  type    = map(string)
  default = {}
}
variable "app_secret_names" {
  type = list(string)
  default = [
    "GOOGLE_CLIENT_ID",
    "GOOGLE_CLIENT_SECRET",
    "KAKAO_CLIENT_ID",
    "KAKAO_CLIENT_SECRET",
    "NAVER_CLIENT_ID",
    "NAVER_CLIENT_SECRET",
    "JWT_SECRET",
    "FCM_SERVICE_ACCOUNT_BASE64",
    "KTO_SERVICE_KEY",
    "KMA_SERVICE_KEY",
    "PUBDATA_FESTIVAL_SERVICE_KEY",
  ]
}
variable "vercel_apex_ip" {
  description = "peakda.com A 레코드가 가리킬 Vercel IP. Vercel 프로젝트 Domains 화면이 표시하는 값을 그대로 넣는다. 빈 문자열이면 레코드를 만들지 않는다"
  type        = string
  default     = ""
}
variable "vercel_www_cname" {
  description = "www.peakda.com CNAME 대상. Vercel 프로젝트마다 고유한 값이므로 Domains 화면 값을 그대로 넣는다. 빈 문자열이면 레코드를 만들지 않는다"
  type        = string
  default     = ""
}
variable "vercel_verification_txt" {
  description = "_vercel TXT 소유 확인 값. Vercel 이 요구할 때만 채운다"
  type        = string
  default     = ""
}
