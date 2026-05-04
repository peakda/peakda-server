package com.peakda.server.infrastructure.external.kto.tatscnctr.response

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CnctrRateItem(
    val baseYmd: String = "",
    val tAtsCd: String = "",
    val tAtsNm: String = "",
    val cnctrRate: String = "",
    val areaCd: String = "",
    val signguCd: String = "",
    val properties: MutableMap<String, String> = mutableMapOf(),
) {
    @JsonAnySetter
    fun put(name: String, value: Any?) {
        if (value != null) properties[name] = value.toString()
    }
}
