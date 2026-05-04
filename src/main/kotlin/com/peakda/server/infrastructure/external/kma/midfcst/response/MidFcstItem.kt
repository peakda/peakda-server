package com.peakda.server.infrastructure.external.kma.midfcst.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class MidFcstItem(
    val wfSv: String = "",
)
