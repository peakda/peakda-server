# Route53 호스팅 존과 A 레코드.
#
# 존은 dev/prod 가 공유한다. dev 구성에서 만들고(create_zone = true),
# prod 는 data source 로 같은 존을 참조해 레코드만 추가한다.
#
# 인증서는 ACM 이 아니라 Caddy 가 Let's Encrypt 로 발급하므로
# 여기서 검증 레코드를 다룰 일은 없다.

resource "aws_route53_zone" "this" {
  count = var.create_zone ? 1 : 0

  name    = var.domain_name
  comment = "peakda 서비스 도메인"

  # 존을 지우면 네임서버가 바뀌어 도메인 전체가 끊긴다.
  lifecycle {
    prevent_destroy = true
  }
}

data "aws_route53_zone" "existing" {
  count = var.create_zone ? 0 : 1

  name         = var.domain_name
  private_zone = false
}

locals {
  zone_id = var.create_zone ? aws_route53_zone.this[0].zone_id : data.aws_route53_zone.existing[0].zone_id
}

resource "aws_route53_record" "app" {
  zone_id = local.zone_id
  name    = var.record_name
  type    = "A"
  ttl     = var.record_ttl
  records = [var.target_ip]
}
