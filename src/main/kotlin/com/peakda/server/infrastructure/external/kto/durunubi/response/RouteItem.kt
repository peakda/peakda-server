package com.peakda.server.infrastructure.external.kto.durunubi.response

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class RouteItem(
    val routeIdx: String = "",
    val routeName: String = "",
    val brdDiv: String = "",
    val themeNm: String = "",
    val sigun: String = "",
    val distance: String = "",
    val requiredTime: String = "",
    val properties: MutableMap<String, String> = mutableMapOf(),
) {
    @JsonAnySetter
    fun put(name: String, value: Any?) {
        if (value != null) properties[name] = value.toString()
    }
}
