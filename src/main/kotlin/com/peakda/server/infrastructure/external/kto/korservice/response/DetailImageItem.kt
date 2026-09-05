package com.peakda.server.infrastructure.external.kto.korservice.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DetailImageItem(
    val contentid: String = "",
    val imgname: String = "",
    val originimgurl: String = "",
    val serialnum: String = "",
    val smallimageurl: String = "",
    val cpyrhtDivCd: String = "",
)
