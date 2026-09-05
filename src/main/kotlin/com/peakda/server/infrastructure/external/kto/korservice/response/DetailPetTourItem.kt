package com.peakda.server.infrastructure.external.kto.korservice.response

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DetailPetTourItem(
    val contentid: String = "",
    val relaAcdntRiskMtr: String = "",
    val acmpyTypeCd: String = "",
    val relaPosesFclty: String = "",
    val relaFrnshPrdlst: String = "",
    val etcAcmpyInfo: String = "",
    val relaPurcPrdlst: String = "",
    val acmpyPsblCpam: String = "",
    val relaRntlPrdlst: String = "",
    val acmpyNeedMtr: String = "",
    val properties: MutableMap<String, String> = mutableMapOf(),
) {
    @JsonAnySetter
    fun put(name: String, value: Any?) {
        if (value != null) properties[name] = value.toString()
    }
}
