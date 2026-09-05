package com.peakda.server.infrastructure.external.kto.korservice.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DetailCommonItem(
    val contentid: String = "",
    val contenttypeid: String = "",
    val title: String = "",
    val tel: String = "",
    val telname: String = "",
    val homepage: String = "",
    val booktour: String = "",
    val firstimage: String = "",
    val firstimage2: String = "",
    val cpyrhtDivCd: String = "",
    val areacode: String = "",
    val sigungucode: String = "",
    val cat1: String = "",
    val cat2: String = "",
    val cat3: String = "",
    val addr1: String = "",
    val addr2: String = "",
    val zipcode: String = "",
    val mapx: String = "",
    val mapy: String = "",
    val mlevel: String = "",
    val overview: String = "",
    val createdtime: String = "",
    val modifiedtime: String = "",
)
