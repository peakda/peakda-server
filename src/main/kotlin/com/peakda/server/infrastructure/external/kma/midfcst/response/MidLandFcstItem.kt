package com.peakda.server.infrastructure.external.kma.midfcst.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class MidLandFcstItem(
    val regId: String = "",
    val rnSt3Am: Int = 0,
    val rnSt3Pm: Int = 0,
    val rnSt4Am: Int = 0,
    val rnSt4Pm: Int = 0,
    val rnSt5Am: Int = 0,
    val rnSt5Pm: Int = 0,
    val rnSt6Am: Int = 0,
    val rnSt6Pm: Int = 0,
    val rnSt7Am: Int = 0,
    val rnSt7Pm: Int = 0,
    val rnSt8: Int = 0,
    val rnSt9: Int = 0,
    val rnSt10: Int = 0,
    val wf3Am: String = "",
    val wf3Pm: String = "",
    val wf4Am: String = "",
    val wf4Pm: String = "",
    val wf5Am: String = "",
    val wf5Pm: String = "",
    val wf6Am: String = "",
    val wf6Pm: String = "",
    val wf7Am: String = "",
    val wf7Pm: String = "",
    val wf8: String = "",
    val wf9: String = "",
    val wf10: String = "",
)
