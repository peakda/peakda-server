# 이미지 버킷 앞단 CDN.
#
# 버킷은 계속 비공개다. OAC 로 이 배포만 GetObject 할 수 있고, 사용자는 CloudFront 를 통해서만 읽는다.
#
# 이 배포가 붙으면 앱이 이미지 URL 을 presigned 대신 `https://{domain}/{objectKey}` 로 내려준다
# (STORAGE_PUBLIC_BASE_URL). presigned 는 응답마다 서명이 바뀌어 CDN 캐시 키가 매번 달라지고,
# 그래서 조회 수만큼 이미지 변환·원본 요청이 쌓였다. 고정 주소라야 캐시가 맞는다.
#
# 객체 key 에는 업로드마다 임의 문자열이 들어간다. 같은 사진은 주소가 영원히 같고 사진이 바뀌면
# 주소도 바뀌므로, 무효화 없이 긴 TTL 을 쓸 수 있다.

terraform {
  required_providers {
    aws = {
      source                = "hashicorp/aws"
      configuration_aliases = [aws.us_east_1]
    }
  }
}

resource "aws_cloudfront_origin_access_control" "this" {
  name                              = "${var.name_prefix}-media-oac"
  description                       = "${var.name_prefix} 이미지 버킷 접근"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

# CloudFront 인증서는 us-east-1 에만 둘 수 있다. ALB 용 인증서(서울)와는 별개다.
resource "aws_acm_certificate" "this" {
  provider = aws.us_east_1

  domain_name       = var.domain_name
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_route53_record" "cert" {
  for_each = {
    for dvo in aws_acm_certificate.this.domain_validation_options : dvo.domain_name => dvo
  }

  zone_id         = var.route53_zone_id
  name            = each.value.resource_record_name
  type            = each.value.resource_record_type
  records         = [each.value.resource_record_value]
  ttl             = 60
  allow_overwrite = true
}

resource "aws_acm_certificate_validation" "this" {
  provider = aws.us_east_1

  certificate_arn         = aws_acm_certificate.this.arn
  validation_record_fqdns = [for record in aws_route53_record.cert : record.fqdn]
}

resource "aws_cloudfront_distribution" "this" {
  enabled     = true
  comment     = "${var.name_prefix} media"
  price_class = var.price_class
  aliases     = [var.domain_name]

  origin {
    origin_id                = "media"
    domain_name              = var.bucket_regional_domain_name
    origin_access_control_id = aws_cloudfront_origin_access_control.this.id
  }

  default_cache_behavior {
    target_origin_id       = "media"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    compress               = true

    # Managed-CachingOptimized: 쿼리스트링·쿠키·헤더를 캐시 키에서 빼고 경로만 본다.
    # 이미지 주소가 곧 캐시 키이므로 이게 맞다.
    cache_policy_id = "658327ea-f89d-4fab-a63d-7e88639e58f6"
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    acm_certificate_arn      = aws_acm_certificate_validation.this.certificate_arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }
}

data "aws_iam_policy_document" "bucket" {
  statement {
    sid       = "AllowCloudFrontRead"
    effect    = "Allow"
    actions   = ["s3:GetObject"]
    resources = ["${var.bucket_arn}/*"]

    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.this.arn]
    }
  }
}

resource "aws_s3_bucket_policy" "media" {
  bucket = var.bucket_id
  policy = data.aws_iam_policy_document.bucket.json
}

resource "aws_route53_record" "cdn" {
  zone_id = var.route53_zone_id
  name    = var.domain_name
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.this.domain_name
    zone_id                = aws_cloudfront_distribution.this.hosted_zone_id
    evaluate_target_health = false
  }
}
