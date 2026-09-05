package com.peakda.server.infrastructure.external.kto.korservice.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SearchStayItem(
    val addr1: String = "",
    val addr2: String = "",
    val areacode: String = "",
    val benikia: String = "",
    val cat1: String = "",
    val cat2: String = "",
    val cat3: String = "",
    val contentid: String = "",
    val contenttypeid: String = "",
    val firstimage: String = "",
    val firstimage2: String = "",
    val goodstay: String = "",
    val hanok: String = "",
    val mapx: String = "",
    val mapy: String = "",
    val modifiedtime: String = "",
    val sigungucode: String = "",
    val tel: String = "",
    val title: String = "",
)
