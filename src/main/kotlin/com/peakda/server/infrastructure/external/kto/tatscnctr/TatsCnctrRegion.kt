package com.peakda.server.infrastructure.external.kto.tatscnctr

/** 집중률 API 가 필수로 요구하는 지역·시군구 코드 쌍. */
data class TatsCnctrRegion(
    val areaCd: String,
    val signguCd: String,
)
