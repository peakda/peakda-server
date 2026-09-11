# ECS environment secrets are snapshots at task startup. Reconcile after a
# credential change, with a schedule to cover delayed or missing events.
locals {
  credential_refresh_name = "${local.name_prefix}-db-credential-refresh"
}

resource "aws_cloudwatch_log_group" "credential_refresh" {
  name              = "/aws/lambda/${local.credential_refresh_name}"
  retention_in_days = 14
}

resource "aws_sqs_queue" "credential_refresh_failures" {
  name                      = "${local.credential_refresh_name}-failures"
  message_retention_seconds = 1209600
  sqs_managed_sse_enabled   = true
}

resource "aws_iam_role" "credential_refresh" {
  name = local.credential_refresh_name
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow", Principal = { Service = "lambda.amazonaws.com" }, Action = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy" "credential_refresh" {
  role = aws_iam_role.credential_refresh.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["secretsmanager:DescribeSecret"]
        Resource = aws_db_instance.this.master_user_secret[0].secret_arn
      },
      {
        Effect   = "Allow"
        Action   = ["ecs:DescribeServices", "ecs:UpdateService"]
        Resource = aws_ecs_service.this.id
      },
      {
        Effect   = "Allow"
        Action   = ["logs:CreateLogStream", "logs:PutLogEvents"]
        Resource = "${aws_cloudwatch_log_group.credential_refresh.arn}:*"
      },
      {
        Effect   = "Allow"
        Action   = ["sqs:SendMessage"]
        Resource = aws_sqs_queue.credential_refresh_failures.arn
      }
    ]
  })
}

resource "aws_lambda_function" "credential_refresh" {
  function_name = local.credential_refresh_name
  role          = aws_iam_role.credential_refresh.arn
  runtime       = "python3.13"
  handler       = "db_credential_refresh.handler"
  # Use bash infra/scripts/terraform-prod.sh plan (or apply/validate) to package first.
  filename         = "${path.module}/build/db-credential-refresh.zip"
  source_code_hash = fileexists("${path.module}/build/db-credential-refresh.zip") ? filebase64sha256("${path.module}/build/db-credential-refresh.zip") : null
  timeout          = 30
  memory_size      = 128
  # Low-quota accounts cannot reserve concurrency without violating AWS's
  # unreserved minimum. Duplicate invocations reconcile against ECS deployments;
  # a racing update can conservatively cause an extra rollout, not skip rotation.
  environment {
    variables = {
      SECRET_ARN  = aws_db_instance.this.master_user_secret[0].secret_arn
      ECS_CLUSTER = aws_ecs_cluster.this.arn
      ECS_SERVICE = aws_ecs_service.this.name
    }
  }
  depends_on = [aws_iam_role_policy.credential_refresh]
  lifecycle {
    precondition {
      condition     = fileexists("${path.module}/build/db-credential-refresh.zip")
      error_message = "Package Lambda before plan/apply: bash infra/scripts/terraform-prod.sh plan (or apply)."
    }
  }
}

resource "aws_lambda_function_event_invoke_config" "credential_refresh" {
  function_name                = aws_lambda_function.credential_refresh.function_name
  maximum_event_age_in_seconds = 3600
  maximum_retry_attempts       = 2
  destination_config {
    on_failure {
      destination = aws_sqs_queue.credential_refresh_failures.arn
    }
  }
}

resource "aws_cloudwatch_event_rule" "credential_changed" {
  name = "${local.credential_refresh_name}-changed"
  event_pattern = jsonencode({
    source        = ["aws.secretsmanager"]
    "detail-type" = ["Secret Label Updated"]
    resources     = [aws_db_instance.this.master_user_secret[0].secret_arn]
    detail        = { labelUpdated = ["AWSCURRENT"] }
  })
}

resource "aws_cloudwatch_event_rule" "credential_reconcile" {
  name                = "${local.credential_refresh_name}-reconcile"
  schedule_expression = "rate(5 minutes)"
}

locals {
  credential_refresh_rules = {
    changed   = aws_cloudwatch_event_rule.credential_changed
    reconcile = aws_cloudwatch_event_rule.credential_reconcile
  }
}

resource "aws_lambda_permission" "credential_refresh" {
  for_each      = local.credential_refresh_rules
  statement_id  = "AllowEventBridge-${each.key}"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.credential_refresh.function_name
  principal     = "events.amazonaws.com"
  source_arn    = each.value.arn
}

resource "aws_sqs_queue_policy" "credential_refresh_failures" {
  queue_url = aws_sqs_queue.credential_refresh_failures.url
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "events.amazonaws.com" }
      Action    = "sqs:SendMessage"
      Resource  = aws_sqs_queue.credential_refresh_failures.arn
      Condition = { ArnEquals = { "aws:SourceArn" = [for rule in local.credential_refresh_rules : rule.arn] } }
    }]
  })
}

resource "aws_cloudwatch_event_target" "credential_refresh" {
  for_each = local.credential_refresh_rules
  rule     = each.value.name
  arn      = aws_lambda_function.credential_refresh.arn
  retry_policy {
    maximum_event_age_in_seconds = 86400
    maximum_retry_attempts       = 185
  }
  dead_letter_config {
    arn = aws_sqs_queue.credential_refresh_failures.arn
  }
  depends_on = [aws_lambda_permission.credential_refresh, aws_sqs_queue_policy.credential_refresh_failures]
}

resource "aws_cloudwatch_metric_alarm" "credential_refresh_errors" {
  alarm_name          = "${local.credential_refresh_name}-errors"
  namespace           = "AWS/Lambda"
  metric_name         = "Errors"
  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  threshold           = 1
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"
  dimensions          = { FunctionName = aws_lambda_function.credential_refresh.function_name }
  alarm_actions       = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "credential_refresh_failures" {
  alarm_name          = "${local.credential_refresh_name}-undelivered"
  namespace           = "AWS/SQS"
  metric_name         = "ApproximateNumberOfMessagesVisible"
  statistic           = "Maximum"
  period              = 300
  evaluation_periods  = 1
  threshold           = 1
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"
  dimensions          = { QueueName = aws_sqs_queue.credential_refresh_failures.name }
  alarm_actions       = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "credential_refresh_missing" {
  alarm_name          = "${local.credential_refresh_name}-not-running"
  namespace           = "AWS/Lambda"
  metric_name         = "Invocations"
  statistic           = "Sum"
  period              = 900
  evaluation_periods  = 2
  threshold           = 1
  comparison_operator = "LessThanThreshold"
  treat_missing_data  = "breaching"
  dimensions          = { FunctionName = aws_lambda_function.credential_refresh.function_name }
  alarm_actions       = [aws_sns_topic.alerts.arn]
}
