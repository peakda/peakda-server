package com.peakda.server.infrastructure.external.kto.durunubi.response

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CourseItem(
    val crsIdx: String = "",
    val crsKorNm: String = "",
    val crsDstnc: String = "",
    val crsTotlRqrmHour: String = "",
    val crsLevel: String = "",
    val sigun: String = "",
    val brdDiv: String = "",
    val routeIdx: String = "",
    val properties: MutableMap<String, String> = mutableMapOf(),
) {
    @JsonAnySetter
    fun put(name: String, value: Any?) {
        if (value != null) properties[name] = value.toString()
    }
}
