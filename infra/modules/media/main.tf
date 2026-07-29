# 앱이 사용하는 이미지 스토리지.
#
# 백업·서버자산 버킷(modules/backup)과는 용도가 완전히 다르다.
# 이쪽은 사용자가 올린 스팟 기록 사진·프로필·큐레이션 이미지가 들어간다.
#
# 읽기는 전부 presigned URL 이므로 버킷을 공개하지 않는다.
# CloudFront 도 붙이지 않는다 — develop 트래픽에는 과한 비용이다.

resource "aws_s3_bucket" "media" {
  bucket = var.bucket_name

  tags = {
    Name = var.bucket_name
  }
}

resource "aws_s3_bucket_public_access_block" "media" {
  bucket = aws_s3_bucket.media.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "media" {
  bucket = aws_s3_bucket.media.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "media" {
  bucket = aws_s3_bucket.media.id

  rule {
    id     = "abort-incomplete-uploads"
    status = "Enabled"

    filter {}

    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }
}

# ---------------------------------------------------------------------------
# 앱 자격증명
#
# EC2 인스턴스 역할을 쓰지 않고 IAM 사용자를 만드는 이유:
# 인스턴스 역할은 임시 자격증명이라 presigned URL 이 세션 만료(수 시간) 시점에
# 무효화된다. 앱은 presigned-url-ttl-seconds = 604800(7일)을 기대하므로
# 7일짜리 서명을 만들려면 장기 자격증명이 필요하다.
#
# 액세스 키는 Terraform 으로 만들지 않는다(state 에 평문으로 남는다).
# apply 후 CLI 로 발급해 SSM SecureString 에 주입한다.
# ---------------------------------------------------------------------------

resource "aws_iam_user" "media" {
  name = var.iam_user_name

  tags = {
    Name = var.iam_user_name
  }
}

data "aws_iam_policy_document" "media" {
  statement {
    sid    = "ObjectAccess"
    effect = "Allow"
    actions = [
      "s3:GetObject",
      "s3:PutObject",
      "s3:DeleteObject",
    ]
    resources = ["${aws_s3_bucket.media.arn}/*"]
  }

  # presigned URL 생성 자체는 API 호출이 아니지만, copy·존재 확인에 필요하다.
  statement {
    sid       = "BucketAccess"
    effect    = "Allow"
    actions   = ["s3:ListBucket"]
    resources = [aws_s3_bucket.media.arn]
  }
}

resource "aws_iam_user_policy" "media" {
  name   = "${var.iam_user_name}-policy"
  user   = aws_iam_user.media.name
  policy = data.aws_iam_policy_document.media.json
}
