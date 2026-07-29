# 애플리케이션 컨테이너 이미지 저장소.
#
# 이미지가 무한정 쌓이면 ECR 스토리지 비용이 늘고 정리도 번거로워지므로
# lifecycle policy 로 최근 N개만 남긴다.

resource "aws_ecr_repository" "this" {
  name                 = var.repository_name
  image_tag_mutability = "MUTABLE" # latest 태그를 매 배포마다 갱신한다

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "AES256"
  }

  tags = {
    Name = var.repository_name
  }
}

resource "aws_ecr_lifecycle_policy" "this" {
  repository = aws_ecr_repository.this.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "최근 ${var.keep_image_count}개 이미지만 보관"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = var.keep_image_count
        }
        action = {
          type = "expire"
        }
      },
    ]
  })
}
