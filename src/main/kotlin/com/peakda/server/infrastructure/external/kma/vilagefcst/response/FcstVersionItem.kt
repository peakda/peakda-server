package com.peakda.server.infrastructure.external.kma.vilagefcst.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class FcstVersionItem(
    val version: String = "",
    val filetype: String = "",
)
