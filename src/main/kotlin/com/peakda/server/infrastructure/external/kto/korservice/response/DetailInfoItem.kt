package com.peakda.server.infrastructure.external.kto.korservice.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DetailInfoItem(
    val contentid: String = "",
    val contenttypeid: String = "",
    val fldgubun: String = "",
    val infoname: String = "",
    val infotext: String = "",
    val serialnum: String = "",
)
