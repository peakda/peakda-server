package com.peakda.server.infrastructure.external.kto.korservice.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class AreaBasedSyncListItem(
    val addr1: String = "",
    val addr2: String = "",
    val areacode: String = "",
    val cat1: String = "",
    val cat2: String = "",
    val cat3: String = "",
    val contentid: String = "",
    val contenttypeid: String = "",
    val createdtime: String = "",
    val firstimage: String = "",
    val firstimage2: String = "",
    val cpyrhtDivCd: String = "",
    val lDongRegnCd: String = "",
    val lDongSignguCd: String = "",
    val lclsSystm1: String = "",
    val lclsSystm2: String = "",
    val lclsSystm3: String = "",
    val mapx: String = "",
    val mapy: String = "",
    val modifiedtime: String = "",
    val sigungucode: String = "",
    val showflag: String = "",
    val tel: String = "",
    val title: String = "",
)
