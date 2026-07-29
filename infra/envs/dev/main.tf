provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project     = "peakda"
      Environment = var.env
      ManagedBy   = "terraform"
    }
  }
}

data "aws_caller_identity" "current" {}

locals {
  name_prefix = "peakda-${var.env}"
  account_id  = data.aws_caller_identity.current.account_id
  app_domain  = "${var.subdomain}.${var.domain_name}"

  # 서버에 배포되는 자산. 내용이 바뀌면 S3 오브젝트가 갱신되고
  # 다음 배포에서 deploy.sh 가 s3 sync 로 가져간다.
  server_assets = {
    "docker-compose.yml" = "${path.module}/../../server/docker-compose.yml"
    "alloy-config.alloy" = "${path.module}/../../server/alloy-config.alloy"
    "Caddyfile"          = "${path.module}/../../server/Caddyfile"
    "deploy.sh"          = "${path.module}/../../server/deploy.sh"
    "backup.sh"          = "${path.module}/../../server/backup.sh"
    "restore.sh"         = "${path.module}/../../server/restore.sh"
  }
}

# ---------------------------------------------------------------------------
# 네트워크
# ---------------------------------------------------------------------------

module "network" {
  source = "../../modules/network"

  name_prefix = local.name_prefix
  vpc_cidr    = var.vpc_cidr
}

# ---------------------------------------------------------------------------
# 컨테이너 레지스트리
# ---------------------------------------------------------------------------

module "registry" {
  source = "../../modules/registry"

  repository_name  = var.ecr_repository_name
  keep_image_count = 5
}

# ---------------------------------------------------------------------------
# 백업·자산 버킷
# ---------------------------------------------------------------------------

module "backup" {
  source = "../../modules/backup"

  bucket_name           = "${local.name_prefix}-storage-${local.account_id}"
  backup_retention_days = 30
}

resource "aws_s3_object" "server_asset" {
  for_each = local.server_assets

  bucket = module.backup.bucket_id
  key    = "assets/${each.key}"
  source = each.value
  etag   = filemd5(each.value)
}

# ---------------------------------------------------------------------------
# 이미지 스토리지
# ---------------------------------------------------------------------------

module "media" {
  source = "../../modules/media"

  bucket_name   = "${local.name_prefix}-media-${local.account_id}"
  iam_user_name = "${local.name_prefix}-media"
}

# ---------------------------------------------------------------------------
# 애플리케이션 설정 (SSM Parameter Store)
# ---------------------------------------------------------------------------

module "config" {
  source = "../../modules/config"

  env = var.env

  parameters = merge(
    {
      # 컨테이너 네트워크 내부 주소. PG·Redis 는 호스트에 포트를 열지 않는다.
      SPRING_DATASOURCE_URL      = "jdbc:postgresql://postgres:5432/peakda"
      SPRING_DATASOURCE_USERNAME = "peakda"
      SPRING_DATA_REDIS_URL      = "redis://redis:6379"

      APP_DOMAIN             = local.app_domain
      PEAKDA_SERVER_DEV      = "https://${local.app_domain}"
      SPRING_PROFILES_ACTIVE = var.env

      # 스케줄러를 켠다. 잡 12종이 새벽~오전에 실행된다.
      EXTERNAL_SCHEDULER_ENABLED  = "true"
      EXTERNAL_QUOTA_ENABLED      = "true"
      EXTERNAL_RATE_LIMIT_ENABLED = "true"
      EXTERNAL_RESILIENCE_ENABLED = "true"

      # 이미지 스토리지. Railway Bucket 관례였던 region=auto, path-style=true 는
      # S3 에서 통하지 않는다. Region.of("auto") 는 엔드포인트 해석이 깨지고,
      # path-style 은 S3 에서 비권장 방식이다.
      STORAGE_BUCKET            = module.media.bucket_id
      STORAGE_ENDPOINT          = "https://s3.${var.region}.amazonaws.com"
      STORAGE_REGION            = var.region
      STORAGE_PATH_STYLE_ACCESS = "false"
    },
    var.app_parameters,
  )

  secret_names = var.app_secret_names
}

# ---------------------------------------------------------------------------
# 앱 서버
# ---------------------------------------------------------------------------

module "app_server" {
  source = "../../modules/app-server"

  name_prefix       = local.name_prefix
  account_id        = local.account_id
  instance_type     = var.instance_type
  subnet_id         = module.network.public_subnet_ids[0]
  availability_zone = module.network.public_subnet_azs[0]
  security_group_id = module.network.app_security_group_id
  root_volume_size  = var.root_volume_size
  data_volume_size  = var.data_volume_size

  ecr_repository_arn = module.registry.repository_arn
  ecr_repository_url = module.registry.repository_url

  parameter_path_prefix = module.config.path_prefix

  backup_bucket_arn = module.backup.bucket_arn
  backup_bucket_id  = module.backup.bucket_id
}

# ---------------------------------------------------------------------------
# DNS
# ---------------------------------------------------------------------------

module "dns" {
  source = "../../modules/dns"

  domain_name = var.domain_name
  create_zone = var.create_dns_zone
  record_name = local.app_domain
  target_ip   = module.app_server.public_ip
}

# ---------------------------------------------------------------------------
# CI/CD 권한
# ---------------------------------------------------------------------------

module "github_oidc" {
  source = "../../modules/github-oidc"

  name_prefix          = local.name_prefix
  region               = var.region
  account_id           = local.account_id
  create_oidc_provider = var.create_oidc_provider
  allowed_subjects     = var.github_allowed_subjects
  ecr_repository_arn   = module.registry.repository_arn
  target_instance_id   = module.app_server.instance_id
}

# ---------------------------------------------------------------------------
# 비용 가드레일
#
# 크레딧이 소진되면 요금이 청구되는 게 아니라 서비스 접근이 종료되므로,
# 예산 초과는 곧 장애다. 알림을 반드시 받는다.
# ---------------------------------------------------------------------------

resource "aws_budgets_budget" "monthly" {
  name         = "${local.name_prefix}-monthly"
  budget_type  = "COST"
  limit_amount = var.monthly_budget_limit
  limit_unit   = "USD"
  time_unit    = "MONTHLY"

  # 실제 사용액이 한도의 80% 를 넘으면 알린다.
  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 80
    threshold_type             = "PERCENTAGE"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = [var.alert_email]
  }

  # 월말 예측치가 한도를 넘을 것 같으면 미리 알린다.
  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 100
    threshold_type             = "PERCENTAGE"
    notification_type          = "FORECASTED"
    subscriber_email_addresses = [var.alert_email]
  }
}
