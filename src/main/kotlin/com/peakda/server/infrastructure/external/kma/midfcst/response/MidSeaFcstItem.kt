package com.peakda.server.infrastructure.external.kma.midfcst.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class MidSeaFcstItem(
    val regId: String = "",
    val wf3Am: String = "",
    val wf3Pm: String = "",
    val wf4Am: String = "",
    val wf4Pm: String = "",
    val wf5Am: String = "",
    val wf5Pm: String = "",
    val wh3AAm: String = "",
    val wh3APm: String = "",
    val wh4AAm: String = "",
    val wh4APm: String = "",
    val wh5AAm: String = "",
    val wh5APm: String = "",
)
