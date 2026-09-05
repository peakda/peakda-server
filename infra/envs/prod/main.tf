provider "aws" {
  region = var.region
  default_tags { tags = { Project = "peakda", Environment = "prod", ManagedBy = "terraform" } }
}

data "aws_caller_identity" "current" {}
data "aws_route53_zone" "root" {
  name         = var.domain_name
  private_zone = false
}
data "aws_ecr_repository" "app" { name = var.ecr_repository_name }
data "aws_iam_openid_connect_provider" "github" {
  count = var.create_oidc_provider ? 0 : 1
  url   = "https://token.actions.githubusercontent.com"
}

locals {
  name_prefix        = "peakda-prod"
  app_domain         = "${var.subdomain}.${var.domain_name}"
  account_id         = data.aws_caller_identity.current.account_id
  parameter_arns     = ["arn:aws:ssm:${var.region}:${local.account_id}:parameter/peakda/prod/*"]
  media_bucket_name  = "${local.name_prefix}-media-${local.account_id}"
  backup_bucket_name = "${local.name_prefix}-storage-${local.account_id}"
}

module "network" {
  source                    = "../../modules/network"
  name_prefix               = local.name_prefix
  vpc_cidr                  = var.vpc_cidr
  create_private_subnets    = true
  create_app_security_group = false
}

resource "aws_security_group" "alb" {
  name   = "${local.name_prefix}-alb-sg"
  vpc_id = module.network.vpc_id
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}
resource "aws_security_group" "task" {
  name   = "${local.name_prefix}-task-sg"
  vpc_id = module.network.vpc_id
  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}
resource "aws_security_group" "migration" {
  name        = "${local.name_prefix}-migration-sg"
  description = "No inbound access; outbound only for one-off migration tasks"
  vpc_id      = module.network.vpc_id
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}
resource "aws_security_group" "db" {
  name   = "${local.name_prefix}-db-sg"
  vpc_id = module.network.vpc_id
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.task.id, aws_security_group.migration.id]
  }
}
resource "aws_security_group" "redis" {
  name   = "${local.name_prefix}-redis-sg"
  vpc_id = module.network.vpc_id
  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.task.id]
  }
}

resource "aws_db_subnet_group" "this" {
  name       = local.name_prefix
  subnet_ids = module.network.private_subnet_ids
}
resource "aws_db_instance" "this" {
  identifier                  = local.name_prefix
  engine                      = "postgres"
  engine_version              = "16"
  instance_class              = var.db_instance_class
  allocated_storage           = 20
  max_allocated_storage       = 50
  storage_type                = "gp3"
  storage_encrypted           = true
  db_name                     = "peakda"
  username                    = "peakda"
  manage_master_user_password = true
  db_subnet_group_name        = aws_db_subnet_group.this.name
  vpc_security_group_ids      = [aws_security_group.db.id]
  publicly_accessible         = false
  multi_az                    = false
  # AWS free-plan account restriction: maximum automated retention is 1 day.
  # S3 custom dumps provide the longer 30-day recovery window.
  backup_retention_period   = 1
  backup_window             = "18:00-19:00"
  maintenance_window        = "sun:19:00-sun:20:00"
  deletion_protection       = true
  delete_automated_backups  = false
  skip_final_snapshot       = false
  final_snapshot_identifier = "${local.name_prefix}-final"
  copy_tags_to_snapshot     = true
}

resource "random_password" "redis" {
  length  = 32
  special = false
}
resource "aws_elasticache_subnet_group" "this" {
  name       = local.name_prefix
  subnet_ids = module.network.private_subnet_ids
}
resource "aws_elasticache_replication_group" "this" {
  replication_group_id       = local.name_prefix
  description                = "PEAKDA production Redis"
  node_type                  = var.redis_node_type
  num_cache_clusters         = 1
  engine                     = "redis"
  engine_version             = "7.1"
  port                       = 6379
  transit_encryption_enabled = true
  auth_token                 = random_password.redis.result
  subnet_group_name          = aws_elasticache_subnet_group.this.name
  security_group_ids         = [aws_security_group.redis.id]
  automatic_failover_enabled = false
  at_rest_encryption_enabled = true
  snapshot_retention_limit   = 1
  apply_immediately          = false
  auto_minor_version_upgrade = true
}

resource "aws_s3_bucket" "media" {
  bucket = local.media_bucket_name
}

resource "aws_s3_bucket_public_access_block" "media" {
  bucket                  = aws_s3_bucket.media.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "media" {
  bucket = aws_s3_bucket.media.id
  rule {
    apply_server_side_encryption_by_default { sse_algorithm = "AES256" }
  }
}

resource "aws_s3_bucket_versioning" "media" {
  bucket = aws_s3_bucket.media.id
  versioning_configuration { status = "Enabled" }
}

resource "aws_s3_bucket_lifecycle_configuration" "media" {
  bucket = aws_s3_bucket.media.id
  rule {
    id     = "abort-incomplete-uploads"
    status = "Enabled"
    filter {}
    abort_incomplete_multipart_upload { days_after_initiation = 7 }
  }
}

resource "aws_s3_bucket" "backup" { bucket = local.backup_bucket_name }

resource "aws_s3_bucket_public_access_block" "backup" {
  bucket                  = aws_s3_bucket.backup.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "backup" {
  bucket = aws_s3_bucket.backup.id
  rule {
    apply_server_side_encryption_by_default { sse_algorithm = "AES256" }
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "backup" {
  bucket = aws_s3_bucket.backup.id
  rule {
    id     = "expire-dumps"
    status = "Enabled"
    filter { prefix = "postgres/" }
    expiration { days = var.backup_retention_days }
  }
  rule {
    id     = "abort-incomplete-uploads"
    status = "Enabled"
    filter {}
    abort_incomplete_multipart_upload { days_after_initiation = 7 }
  }
}

resource "aws_lb" "this" {
  name                       = local.name_prefix
  load_balancer_type         = "application"
  subnets                    = module.network.public_subnet_ids
  security_groups            = [aws_security_group.alb.id]
  enable_deletion_protection = true
}
resource "aws_lb_target_group" "this" {
  name                 = local.name_prefix
  port                 = 8080
  protocol             = "HTTP"
  vpc_id               = module.network.vpc_id
  target_type          = "ip"
  deregistration_delay = 30
  health_check {
    path                = "/actuator/health/readiness"
    matcher             = "200"
    interval            = 15
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}
resource "aws_acm_certificate" "this" {
  domain_name       = local.app_domain
  validation_method = "DNS"
  lifecycle { create_before_destroy = true }
}
resource "aws_route53_record" "cert" {
  for_each        = { for dvo in aws_acm_certificate.this.domain_validation_options : dvo.domain_name => dvo }
  zone_id         = data.aws_route53_zone.root.zone_id
  name            = each.value.resource_record_name
  type            = each.value.resource_record_type
  records         = [each.value.resource_record_value]
  ttl             = 60
  allow_overwrite = true
}
resource "aws_acm_certificate_validation" "this" {
  certificate_arn         = aws_acm_certificate.this.arn
  validation_record_fqdns = [for record in aws_route53_record.cert : record.fqdn]
}
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.this.arn
  port              = 80
  protocol          = "HTTP"
  default_action {
    type = "redirect"
    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}
resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.this.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = aws_acm_certificate_validation.this.certificate_arn
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.this.arn
  }
}
resource "aws_lb_listener_rule" "actuator_health" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 10
  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.this.arn
  }
  condition {
    path_pattern { values = ["/actuator/health", "/actuator/health/*"] }
  }
}
resource "aws_lb_listener_rule" "actuator_block" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 20
  action {
    type = "fixed-response"
    fixed_response {
      content_type = "text/plain"
      message_body = "Not Found"
      status_code  = "404"
    }
  }
  condition {
    path_pattern { values = ["/actuator/*"] }
  }
}
resource "aws_route53_record" "app" {
  zone_id = data.aws_route53_zone.root.zone_id
  name    = local.app_domain
  type    = "A"
  alias {
    name                   = aws_lb.this.dns_name
    zone_id                = aws_lb.this.zone_id
    evaluate_target_health = true
  }
}

resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/${local.name_prefix}"
  retention_in_days = 14
}
resource "aws_ecs_cluster" "this" {
  name = local.name_prefix
  setting {
    name  = "containerInsights"
    value = "enhanced"
  }
}
resource "aws_iam_role" "execution" {
  name               = "${local.name_prefix}-ecs-execution"
  assume_role_policy = jsonencode({ Version = "2012-10-17", Statement = [{ Effect = "Allow", Principal = { Service = "ecs-tasks.amazonaws.com" }, Action = "sts:AssumeRole" }] })
}
resource "aws_iam_role_policy_attachment" "execution" {
  role       = aws_iam_role.execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}
resource "aws_iam_role_policy" "execution_secrets" {
  role = aws_iam_role.execution.id
  policy = jsonencode({ Version = "2012-10-17", Statement = [
    { Effect = "Allow", Action = ["ssm:GetParameters", "ssm:GetParameter"], Resource = concat(local.parameter_arns, [aws_ssm_parameter.redis_url.arn]) },
    { Effect = "Allow", Action = ["secretsmanager:GetSecretValue"], Resource = [aws_db_instance.this.master_user_secret[0].secret_arn] },
    { Effect = "Allow", Action = ["kms:Decrypt"], Resource = "*", Condition = { StringEquals = { "kms:ViaService" = "ssm.${var.region}.amazonaws.com" } } },
    { Effect = "Allow", Action = ["kms:Decrypt"], Resource = "*", Condition = { StringEquals = { "kms:ViaService" = "secretsmanager.${var.region}.amazonaws.com" } } }
  ] })
}
resource "aws_iam_role" "task" {
  name               = "${local.name_prefix}-ecs-task"
  assume_role_policy = jsonencode({ Version = "2012-10-17", Statement = [{ Effect = "Allow", Principal = { Service = "ecs-tasks.amazonaws.com" }, Action = "sts:AssumeRole" }] })
}
resource "aws_iam_role_policy" "task" {
  role = aws_iam_role.task.id
  policy = jsonencode({ Version = "2012-10-17", Statement = [
    { Effect = "Allow", Action = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"], Resource = ["${aws_s3_bucket.media.arn}/*"] },
    { Effect = "Allow", Action = ["s3:ListBucket"], Resource = [aws_s3_bucket.media.arn] },
    { Effect = "Allow", Action = ["s3:GetObject"], Resource = ["${aws_s3_bucket.backup.arn}/migration/*", "${aws_s3_bucket.backup.arn}/postgres/*"] },
    { Effect = "Allow", Action = ["s3:PutObject"], Resource = ["${aws_s3_bucket.backup.arn}/postgres/*"] },
    { Effect = "Allow", Action = ["s3:ListBucket"], Resource = [aws_s3_bucket.backup.arn] }
  ] })
}

resource "aws_ssm_parameter" "redis_url" {
  name  = "/peakda/prod/SPRING_DATA_REDIS_URL"
  type  = "SecureString"
  value = "rediss://:${random_password.redis.result}@${aws_elasticache_replication_group.this.primary_endpoint_address}:6379"
}

locals {
  app_parameters = merge({
    SPRING_PROFILES_ACTIVE                                 = "prod"
    SPRING_DATASOURCE_URL                                  = "jdbc:postgresql://${aws_db_instance.this.address}:5432/peakda?sslmode=require"
    SPRING_DATASOURCE_USERNAME                             = "peakda"
    APP_DOMAIN                                             = local.app_domain
    COOKIE_DOMAIN                                          = ".${var.domain_name}"
    COOKIE_SECURE                                          = "true"
    COOKIE_SAME_SITE                                       = "None"
    CORS_ALLOWED_ORIGINS                                   = var.cors_allowed_origins
    OAUTH2_REDIRECT_URI                                    = var.oauth2_redirect_uri
    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_SCOPE = "profile_image,account_email"
    EXTERNAL_SCHEDULER_ENABLED                             = "true"
    EXTERNAL_QUOTA_ENABLED                                 = "true"
    EXTERNAL_RATE_LIMIT_ENABLED                            = "true"
    EXTERNAL_RESILIENCE_ENABLED                            = "true"
    STORAGE_BUCKET                                         = local.media_bucket_name
    STORAGE_ENDPOINT                                       = "https://s3.${var.region}.amazonaws.com"
    STORAGE_REGION                                         = var.region
    STORAGE_PATH_STYLE_ACCESS                              = "false"
    STORAGE_PRESIGNED_URL_TTL_SECONDS                      = "3600"
    JAVA_OPTS                                              = "-Xms512m -Xmx1024m -XX:MaxRAMPercentage=70.0"
    FCM_ENABLED                                            = "true"
    FCM_PROJECT_ID                                         = var.fcm_project_id
  }, var.app_parameters)
}

module "config" {
  source       = "../../modules/config"
  env          = "prod"
  parameters   = local.app_parameters
  secret_names = var.app_secret_names
}

resource "aws_ecs_task_definition" "this" {
  family                   = local.name_prefix
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "ARM64"
  }
  execution_role_arn    = aws_iam_role.execution.arn
  task_role_arn         = aws_iam_role.task.arn
  container_definitions = jsonencode([{ name = "app", image = "${data.aws_ecr_repository.app.repository_url}:${var.image_tag}", essential = true, portMappings = [{ containerPort = 8080, protocol = "tcp" }], environment = [for name, value in local.app_parameters : { name = name, value = value }], secrets = concat([{ name = "SPRING_DATASOURCE_PASSWORD", valueFrom = "${aws_db_instance.this.master_user_secret[0].secret_arn}:password::" }, { name = "SPRING_DATA_REDIS_URL", valueFrom = aws_ssm_parameter.redis_url.arn }], [for name in var.app_secret_names : { name = name, valueFrom = module.config.secret_arns[name] } if name != "SPRING_DATASOURCE_PASSWORD" && name != "SPRING_DATA_REDIS_URL"]), logConfiguration = { logDriver = "awslogs", options = { "awslogs-group" = aws_cloudwatch_log_group.app.name, "awslogs-region" = var.region, "awslogs-stream-prefix" = "app" } }, healthCheck = { command = ["CMD-SHELL", "curl -fsS http://localhost:8080/actuator/health/readiness || exit 1"], interval = 30, timeout = 5, retries = 3, startPeriod = 60 } }])
}
resource "aws_ecs_task_definition" "migration" {
  family                   = "${local.name_prefix}-migration"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "ARM64"
  }
  execution_role_arn = aws_iam_role.execution.arn
  task_role_arn      = aws_iam_role.task.arn
  container_definitions = jsonencode([{
    name      = "migration"
    image     = "${data.aws_ecr_repository.app.repository_url}:${var.migration_image_tag}"
    essential = true
    environment = [
      { name = "PGHOST", value = aws_db_instance.this.address },
      { name = "PGPORT", value = "5432" },
      { name = "PGDATABASE", value = "peakda" },
      { name = "PGUSER", value = "peakda" },
      { name = "MIGRATION_SOURCE_S3_URI", value = var.migration_source_s3_uri },
      { name = "MIGRATION_ARTIFACT_S3_PREFIX", value = "s3://${local.backup_bucket_name}/postgres/migration-runs" }
    ]
    secrets = [{ name = "PGPASSWORD", valueFrom = "${aws_db_instance.this.master_user_secret[0].secret_arn}:password::" }]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.app.name
        "awslogs-region"        = var.region
        "awslogs-stream-prefix" = "migration"
      }
    }
  }])
}
resource "aws_ecs_service" "this" {
  name                               = local.name_prefix
  cluster                            = aws_ecs_cluster.this.id
  task_definition                    = aws_ecs_task_definition.this.arn
  desired_count                      = var.desired_count
  launch_type                        = "FARGATE"
  platform_version                   = "LATEST"
  availability_zone_rebalancing      = "ENABLED"
  health_check_grace_period_seconds  = 180
  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 150
  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }
  network_configuration {
    subnets          = module.network.public_subnet_ids
    security_groups  = [aws_security_group.task.id]
    assign_public_ip = true
  }
  load_balancer {
    target_group_arn = aws_lb_target_group.this.arn
    container_name   = "app"
    container_port   = 8080
  }
  depends_on = [aws_lb_listener.https, aws_iam_role_policy_attachment.execution]

  lifecycle {
    # Application deployments register immutable task-definition revisions in CI.
    # Terraform owns the infrastructure shape, not the currently deployed image revision.
    ignore_changes = [task_definition]
  }
}
resource "aws_appautoscaling_target" "ecs" {
  min_capacity       = var.min_capacity
  max_capacity       = var.max_capacity
  resource_id        = "service/${aws_ecs_cluster.this.name}/${aws_ecs_service.this.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}
resource "aws_appautoscaling_policy" "cpu" {
  name               = "${local.name_prefix}-cpu"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs.service_namespace
  target_tracking_scaling_policy_configuration {
    predefined_metric_specification { predefined_metric_type = "ECSServiceAverageCPUUtilization" }
    target_value       = 60
    scale_in_cooldown  = 120
    scale_out_cooldown = 60
  }
}
resource "aws_appautoscaling_policy" "alb" {
  name               = "${local.name_prefix}-requests"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs.service_namespace
  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ALBRequestCountPerTarget"
      resource_label         = "${aws_lb.this.arn_suffix}/${aws_lb_target_group.this.arn_suffix}"
    }
    target_value       = var.alb_requests_per_target
    scale_in_cooldown  = 180
    scale_out_cooldown = 60
  }
}

data "aws_iam_policy_document" "github_assume" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    principals {
      type        = "Federated"
      identifiers = [var.create_oidc_provider ? aws_iam_openid_connect_provider.github[0].arn : data.aws_iam_openid_connect_provider.github[0].arn]
    }
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }
    condition {
      test     = "StringLike"
      variable = "token.actions.githubusercontent.com:sub"
      values   = var.github_allowed_subjects
    }
  }
}
resource "aws_iam_openid_connect_provider" "github" {
  count           = var.create_oidc_provider ? 1 : 0
  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = ["6938fd4d98bab03faadb97b34396831e3780aea1"]
}
resource "aws_iam_role" "github_deploy" {
  name               = "${local.name_prefix}-github-deploy"
  assume_role_policy = data.aws_iam_policy_document.github_assume.json
}
resource "aws_iam_role_policy" "github_deploy" {
  role = aws_iam_role.github_deploy.id
  policy = jsonencode({ Version = "2012-10-17", Statement = [
    { Effect = "Allow", Action = ["ecr:GetAuthorizationToken"], Resource = "*" },
    { Effect = "Allow", Action = ["ecr:BatchCheckLayerAvailability", "ecr:BatchGetImage", "ecr:CompleteLayerUpload", "ecr:GetDownloadUrlForLayer", "ecr:InitiateLayerUpload", "ecr:PutImage", "ecr:UploadLayerPart"], Resource = data.aws_ecr_repository.app.arn },
    { Effect = "Allow", Action = ["ecs:RegisterTaskDefinition"], Resource = "*" },
    { Effect = "Allow", Action = ["ecs:UpdateService", "ecs:DescribeServices", "ecs:DescribeTaskDefinition", "ecs:ListTasks", "ecs:DescribeTasks"], Resource = [aws_ecs_cluster.this.arn, aws_ecs_service.this.arn, "arn:aws:ecs:${var.region}:${local.account_id}:task-definition/${local.name_prefix}:*"] },
    { Effect = "Allow", Action = ["iam:PassRole"], Resource = [aws_iam_role.execution.arn, aws_iam_role.task.arn] }
  ] })
}

resource "aws_sns_topic" "alerts" { name = "${local.name_prefix}-alerts" }
resource "aws_sns_topic_subscription" "alerts_email" {
  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "email"
  endpoint  = var.alert_email
}
resource "aws_budgets_budget" "monthly" {
  name         = "${local.name_prefix}-monthly"
  budget_type  = "COST"
  limit_amount = var.monthly_budget_limit
  limit_unit   = "USD"
  time_unit    = "MONTHLY"
  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 80
    threshold_type             = "PERCENTAGE"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = [var.alert_email]
  }
  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 100
    threshold_type             = "PERCENTAGE"
    notification_type          = "FORECASTED"
    subscriber_email_addresses = [var.alert_email]
  }
}

resource "aws_cloudwatch_metric_alarm" "alb_5xx" {
  alarm_name          = "${local.name_prefix}-alb-5xx"
  namespace           = "AWS/ApplicationELB"
  metric_name         = "HTTPCode_Target_5XX_Count"
  statistic           = "Sum"
  period              = 60
  evaluation_periods  = 3
  threshold           = 5
  comparison_operator = "GreaterThanThreshold"
  dimensions          = { LoadBalancer = aws_lb.this.arn_suffix }
  alarm_actions       = [aws_sns_topic.alerts.arn]
}
resource "aws_cloudwatch_metric_alarm" "unhealthy_hosts" {
  alarm_name          = "${local.name_prefix}-unhealthy-hosts"
  namespace           = "AWS/ApplicationELB"
  metric_name         = "UnHealthyHostCount"
  statistic           = "Maximum"
  period              = 60
  evaluation_periods  = 2
  threshold           = 1
  comparison_operator = "GreaterThanOrEqualToThreshold"
  dimensions = {
    LoadBalancer = aws_lb.this.arn_suffix
    TargetGroup  = aws_lb_target_group.this.arn_suffix
  }
  alarm_actions = [aws_sns_topic.alerts.arn]
}
