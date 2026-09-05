package com.peakda.server.infrastructure.external.kma.vilagefcst.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class VilageFcstItem(
    val baseDate: String = "",
    val baseTime: String = "",
    val category: String = "",
    val fcstDate: String = "",
    val fcstTime: String = "",
    val fcstValue: String = "",
    val nx: Int = 0,
    val ny: Int = 0,
)
