# 백업과 서버 자산을 함께 담는 버킷.
#
# 두 용도를 프리픽스로 나눈다.
#   postgres/ — pg_dump 결과. 보존 기간이 지나면 삭제한다.
#   assets/   — docker-compose.yml, Caddyfile, deploy.sh 등 서버 구성 파일. 영구 보관.
#
# 버킷을 하나로 합치는 이유는 develop 환경에서 버킷을 늘려 관리 포인트를
# 만들 이유가 없기 때문이다. lifecycle 은 프리픽스 필터로 분리한다.

resource "aws_s3_bucket" "this" {
  bucket = var.bucket_name

  tags = {
    Name = var.bucket_name
  }
}

resource "aws_s3_bucket_public_access_block" "this" {
  bucket = aws_s3_bucket.this.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "this" {
  bucket = aws_s3_bucket.this.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "this" {
  bucket = aws_s3_bucket.this.id

  # DB 덤프만 만료시킨다. assets/ 는 서버가 부팅·배포 때마다 읽어가므로 유지한다.
  rule {
    id     = "expire-postgres-dumps"
    status = "Enabled"

    filter {
      prefix = "postgres/"
    }

    expiration {
      days = var.backup_retention_days
    }
  }

  rule {
    id     = "abort-incomplete-uploads"
    status = "Enabled"

    filter {}

    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }
}
