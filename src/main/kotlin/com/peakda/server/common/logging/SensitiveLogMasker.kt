package com.peakda.server.common.logging

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 로그로 나가는 `name=value` 문자열에서 민감한 값을 가린다.
 *
 * 컨트롤러 파라미터는 data class `toString()` 형태(`Request(field=value, ...)`)로 찍히므로
 * 필드 이름으로 값을 찾는다. 로그는 외부(Grafana Cloud)로 전송된다.
 *
 * - 토큰·시크릿·비밀번호·OAuth 교환 코드: 값을 통째로 가린다
 * - 좌표: 소수 둘째 자리(약 1km)까지만 남긴다
 * - 이메일: 첫 글자와 도메인만 남긴다
 */
object SensitiveLogMasker {

    private const val MASK = "***"
    private const val COORDINATE_SCALE = 2

    private val SECRET = Regex("""\b(\w*(?:[Tt]oken|[Ss]ecret|[Pp]assword)|code)=([^,)\s]+)""")
    private val COORDINATE = Regex("""\b((?:min|max)?(?:[Ll]at|[Ll]ng)|latitude|longitude)=(-?\d+(?:\.\d+)?)""")
    private val EMAIL = Regex("""\b(email)=([^,)\s@]+)@([^,)\s]+)""")

    fun mask(text: String): String = text
        .replace(SECRET) { "${it.groupValues[1]}=$MASK" }
        .replace(COORDINATE) { "${it.groupValues[1]}=${coarse(it.groupValues[2])}" }
        .replace(EMAIL) { "${it.groupValues[1]}=${it.groupValues[2].take(1)}$MASK@${it.groupValues[3]}" }

    private fun coarse(value: String): String =
        BigDecimal(value).setScale(COORDINATE_SCALE, RoundingMode.HALF_UP).toPlainString()
}
