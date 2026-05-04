package com.peakda.server.infrastructure.external.kma.vilagefcst.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class UltraSrtNcstItem(
    val baseDate: String = "",
    val baseTime: String = "",
    val category: String = "",
    val obsrValue: String = "",
    val nx: Int = 0,
    val ny: Int = 0,
)
