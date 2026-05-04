package com.peakda.server.infrastructure.external.kto.korservice.response

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DetailIntroItem(
    val contentid: String = "",
    val contenttypeid: String = "",
    val accomcount: String = "",
    val chkbabycarriage: String = "",
    val chkcreditcard: String = "",
    val chkpet: String = "",
    val expagerange: String = "",
    val expguide: String = "",
    val heritage1: String = "",
    val heritage2: String = "",
    val heritage3: String = "",
    val infocenter: String = "",
    val opendate: String = "",
    val parking: String = "",
    val restdate: String = "",
    val useseason: String = "",
    val usetime: String = "",
    val properties: MutableMap<String, String> = mutableMapOf(),
) {
    @JsonAnySetter
    fun put(name: String, value: Any?) {
        if (value != null) properties[name] = value.toString()
    }
}
