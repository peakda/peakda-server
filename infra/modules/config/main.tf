# 애플리케이션 환경변수를 SSM Parameter Store 로 관리한다.
#
# 표준 파라미터는 무료이고, SecureString 도 기본 KMS 키를 쓰면 추가 비용이 없다.
# 경로 규약은 /peakda/<env>/<KEY> 이며, 서버의 deploy.sh 가
# get-parameters-by-path 로 한 번에 받아 .env 를 만든다.
#
# 시크릿은 값을 Terraform 에 두지 않는다. placeholder 로 만들고 ignore_changes 를 걸어
# 실제 값은 `aws ssm put-parameter --overwrite` 로 1회 주입한다.
# state 파일에 평문 시크릿이 남지 않게 하기 위함이다.

locals {
  prefix = "/peakda/${var.env}"
}

resource "aws_ssm_parameter" "plain" {
  for_each = var.parameters

  name  = "${local.prefix}/${each.key}"
  type  = "String"
  value = each.value
  tier  = "Standard"

  tags = {
    Name = each.key
  }
}

resource "aws_ssm_parameter" "secret" {
  for_each = toset(var.secret_names)

  name  = "${local.prefix}/${each.value}"
  type  = "SecureString"
  value = "PLACEHOLDER_SET_ME_VIA_CLI"
  tier  = "Standard"

  tags = {
    Name = each.value
  }

  # 실제 시크릿은 CLI 로 주입한다. Terraform 이 placeholder 로 되돌리면 안 된다.
  lifecycle {
    ignore_changes = [value]
  }
}
