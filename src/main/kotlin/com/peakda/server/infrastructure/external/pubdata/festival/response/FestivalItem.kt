package com.peakda.server.infrastructure.external.pubdata.festival.response

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class FestivalItem(
    val fstvlNm: String = "",
    val opar: String = "",
    val fstvlStartDate: String = "",
    val fstvlEndDate: String = "",
    val fstvlCo: String = "",
    val mnnstNm: String = "",
    val auspcInsttNm: String = "",
    val suprtInsttNm: String = "",
    val phoneNumber: String = "",
    val homepageUrl: String = "",
    val relateInfo: String = "",
    val rdnmadr: String = "",
    val lnmadr: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val referenceDate: String = "",
    val insttCode: String = "",
    val insttNm: String = "",
    val properties: MutableMap<String, String> = mutableMapOf(),
) {
    @JsonAnySetter
    fun put(name: String, value: Any?) {
        if (value != null) properties[name] = value.toString()
    }
}
