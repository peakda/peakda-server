package com.peakda.server.infrastructure.external.kma.midfcst.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class MidTaItem(
    val regId: String = "",
    val taMin3: Int = 0,
    val taMin3Low: Int = 0,
    val taMin3High: Int = 0,
    val taMax3: Int = 0,
    val taMax3Low: Int = 0,
    val taMax3High: Int = 0,
    val taMin4: Int = 0,
    val taMax4: Int = 0,
    val taMin5: Int = 0,
    val taMax5: Int = 0,
    val taMin6: Int = 0,
    val taMax6: Int = 0,
    val taMin7: Int = 0,
    val taMax7: Int = 0,
    val taMin8: Int = 0,
    val taMax8: Int = 0,
    val taMin9: Int = 0,
    val taMax9: Int = 0,
    val taMin10: Int = 0,
    val taMax10: Int = 0,
)
