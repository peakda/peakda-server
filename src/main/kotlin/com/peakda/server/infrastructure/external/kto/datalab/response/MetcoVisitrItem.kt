package com.peakda.server.infrastructure.external.kto.datalab.response

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class MetcoVisitrItem(
    val baseYmd: String = "",
    val areaCd: String = "",
    val areaNm: String = "",
    val touDivCd: String = "",
    val touDivNm: String = "",
    val num: String = "",
    val properties: MutableMap<String, String> = mutableMapOf(),
) {
    @JsonAnySetter
    fun put(name: String, value: Any?) {
        if (value != null) properties[name] = value.toString()
    }
}
