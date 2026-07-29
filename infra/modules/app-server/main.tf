# 앱 서버 EC2 한 대.
#
# 앱·PostgreSQL·Redis·Caddy 를 docker compose 로 함께 올린다.
# 데이터(PG/Redis)는 root 볼륨이 아니라 별도 EBS 볼륨에 둔다.
# 인스턴스를 교체하거나 타입을 상향해도 데이터가 남게 하기 위함이다.

data "aws_ssm_parameter" "ami" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-arm64"
}

data "aws_region" "current" {}

# ---------------------------------------------------------------------------
# IAM
# ---------------------------------------------------------------------------

resource "aws_iam_role" "this" {
  name = "${var.name_prefix}-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

# SSM Session Manager 접속과 Send-Command 배포에 필요하다. SSH 를 열지 않는 근거.
resource "aws_iam_role_policy_attachment" "ssm_core" {
  role       = aws_iam_role.this.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

data "aws_iam_policy_document" "instance" {
  # ECR 로그인 토큰은 리소스 단위 제한이 불가능하다.
  statement {
    sid       = "EcrAuth"
    effect    = "Allow"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid    = "EcrPull"
    effect = "Allow"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:BatchGetImage",
      "ecr:GetDownloadUrlForLayer",
    ]
    resources = [var.ecr_repository_arn]
  }

  statement {
    sid    = "ReadAppConfig"
    effect = "Allow"
    actions = [
      "ssm:GetParameter",
      "ssm:GetParameters",
      "ssm:GetParametersByPath",
    ]
    resources = [
      "arn:aws:ssm:${data.aws_region.current.region}:${var.account_id}:parameter${var.parameter_path_prefix}",
      "arn:aws:ssm:${data.aws_region.current.region}:${var.account_id}:parameter${var.parameter_path_prefix}/*",
    ]
  }

  # SecureString 복호화. 기본 KMS 키(alias/aws/ssm)로 한정한다.
  statement {
    sid       = "DecryptSecureString"
    effect    = "Allow"
    actions   = ["kms:Decrypt"]
    resources = ["*"]

    condition {
      test     = "StringEquals"
      variable = "kms:ViaService"
      values   = ["ssm.${data.aws_region.current.region}.amazonaws.com"]
    }
  }

  statement {
    sid       = "WriteBackups"
    effect    = "Allow"
    actions   = ["s3:PutObject"]
    resources = ["${var.backup_bucket_arn}/postgres/*"]
  }

  statement {
    sid     = "ReadServerAssets"
    effect  = "Allow"
    actions = ["s3:GetObject"]
    resources = [
      "${var.backup_bucket_arn}/assets/*",
      # 데이터 이관 덤프. restore.sh 가 이 경로에서 내려받는다.
      "${var.backup_bucket_arn}/migration/*",
      # 복구 시 과거 백업을 되읽어야 한다.
      "${var.backup_bucket_arn}/postgres/*",
    ]
  }

  statement {
    sid       = "ListBucket"
    effect    = "Allow"
    actions   = ["s3:ListBucket"]
    resources = [var.backup_bucket_arn]
  }
}

resource "aws_iam_role_policy" "this" {
  name   = "${var.name_prefix}-ec2-policy"
  role   = aws_iam_role.this.id
  policy = data.aws_iam_policy_document.instance.json
}

resource "aws_iam_instance_profile" "this" {
  name = "${var.name_prefix}-ec2-profile"
  role = aws_iam_role.this.name
}

# ---------------------------------------------------------------------------
# 데이터 볼륨
# ---------------------------------------------------------------------------

resource "aws_ebs_volume" "data" {
  availability_zone = var.availability_zone
  size              = var.data_volume_size
  type              = "gp3"
  encrypted         = true

  tags = {
    Name = "${var.name_prefix}-data"
  }

  # PG 데이터가 들어있다. 실수로 지워지면 백업에서 복구해야 한다.
  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_volume_attachment" "data" {
  device_name = "/dev/sdf"
  volume_id   = aws_ebs_volume.data.id
  instance_id = aws_instance.this.id
}

# ---------------------------------------------------------------------------
# 인스턴스
# ---------------------------------------------------------------------------

resource "aws_instance" "this" {
  ami                    = data.aws_ssm_parameter.ami.value
  instance_type          = var.instance_type
  subnet_id              = var.subnet_id
  vpc_security_group_ids = [var.security_group_id]
  iam_instance_profile   = aws_iam_instance_profile.this.name
  availability_zone      = var.availability_zone

  root_block_device {
    volume_size           = var.root_volume_size
    volume_type           = "gp3"
    encrypted             = true
    delete_on_termination = true
  }

  # IMDSv2 강제 — SSRF 로 인스턴스 자격증명이 새는 경로를 막는다.
  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 2 # 컨테이너에서 메타데이터 접근에 필요
  }

  user_data = templatefile("${path.module}/user_data.sh.tftpl", {
    region           = data.aws_region.current.region
    data_volume_id   = replace(aws_ebs_volume.data.id, "-", "")
    assets_bucket    = var.backup_bucket_id
    parameter_prefix = var.parameter_path_prefix
    ecr_repository   = var.ecr_repository_url
    compose_project  = var.name_prefix
  })

  # user_data 를 바꿔도 인스턴스를 재생성하지 않는다.
  # 재생성되면 root 볼륨의 도커 이미지가 날아가고 다운타임이 길어진다.
  # 변경 사항은 자산을 S3 에 올린 뒤 SSM 으로 스크립트를 재실행해 반영한다.
  user_data_replace_on_change = false

  tags = {
    Name = "${var.name_prefix}-app"
  }

  lifecycle {
    ignore_changes = [ami] # AMI 갱신으로 인스턴스가 교체되지 않도록 한다
  }
}

resource "aws_eip" "this" {
  instance = aws_instance.this.id
  domain   = "vpc"

  tags = {
    Name = "${var.name_prefix}-eip"
  }
}
