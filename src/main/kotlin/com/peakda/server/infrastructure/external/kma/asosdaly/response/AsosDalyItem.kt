package com.peakda.server.infrastructure.external.kma.asosdaly.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class AsosDalyItem(
    val tm: String = "",
    val stnId: String = "",
    val stnNm: String = "",
    // ASOS 수치 필드는 결측일 때 null 대신 빈 문자열을 내려주므로 문자열로 역직렬화한다.
    val avgTa: String = "",
    val minTa: String = "",
    val maxTa: String = "",
)
