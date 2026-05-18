package com.peakda.server.infrastructure.external.kto.korservice

import com.fasterxml.jackson.databind.ObjectMapper
import com.peakda.server.infrastructure.external.datagokr.DataGoKrBody
import com.peakda.server.infrastructure.external.datagokr.DataGoKrErrorDecoder
import com.peakda.server.infrastructure.external.common.ExternalApiResilienceExecutor
import com.peakda.server.infrastructure.external.datagokr.getDataGoKrBody
import com.peakda.server.infrastructure.external.kto.korservice.response.AreaBasedListItem
import com.peakda.server.infrastructure.external.kto.korservice.response.AreaBasedSyncListItem
import com.peakda.server.infrastructure.external.kto.korservice.response.DetailCommonItem
import com.peakda.server.infrastructure.external.kto.korservice.response.DetailImageItem
import com.peakda.server.infrastructure.external.kto.korservice.response.DetailInfoItem
import com.peakda.server.infrastructure.external.kto.korservice.response.DetailIntroItem
import com.peakda.server.infrastructure.external.kto.korservice.response.DetailPetTourItem
import com.peakda.server.infrastructure.external.kto.korservice.response.LocationBasedListItem
import com.peakda.server.infrastructure.external.kto.korservice.response.SearchFestivalItem
import com.peakda.server.infrastructure.external.kto.korservice.response.SearchKeywordItem
import com.peakda.server.infrastructure.external.kto.korservice.response.SearchStayItem
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class KorServiceClient(
    @param:Qualifier("korServiceRestClient") private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
    private val errorDecoder: DataGoKrErrorDecoder,
    private val resilience: ExternalApiResilienceExecutor,
) {
    fun areaBasedList(params: Map<String, Any?>): DataGoKrBody<AreaBasedListItem> =
        get("/areaBasedList2", params)

    fun locationBasedList(params: Map<String, Any?>): DataGoKrBody<LocationBasedListItem> =
        get("/locationBasedList2", params)

    fun searchKeyword(params: Map<String, Any?>): DataGoKrBody<SearchKeywordItem> =
        get("/searchKeyword2", params)

    fun searchFestival(params: Map<String, Any?>): DataGoKrBody<SearchFestivalItem> =
        get("/searchFestival2", params)

    fun searchStay(params: Map<String, Any?>): DataGoKrBody<SearchStayItem> =
        get("/searchStay2", params)

    fun detailCommon(params: Map<String, Any?>): DataGoKrBody<DetailCommonItem> =
        get("/detailCommon2", params)

    fun detailIntro(params: Map<String, Any?>): DataGoKrBody<DetailIntroItem> =
        get("/detailIntro2", params)

    fun detailInfo(params: Map<String, Any?>): DataGoKrBody<DetailInfoItem> =
        get("/detailInfo2", params)

    fun detailImage(params: Map<String, Any?>): DataGoKrBody<DetailImageItem> =
        get("/detailImage2", params)

    fun areaBasedSyncList(params: Map<String, Any?>): DataGoKrBody<AreaBasedSyncListItem> =
        get("/areaBasedSyncList2", params)

    fun detailPetTour(params: Map<String, Any?>): DataGoKrBody<DetailPetTourItem> =
        get("/detailPetTour2", params)

    private inline fun <reified T : Any> get(path: String, params: Map<String, Any?>): DataGoKrBody<T> {
        return resilience.execute(PROVIDER) {
            restClient.getDataGoKrBody<T>(objectMapper, errorDecoder, path, params)
        }
    }

    companion object {
        private const val PROVIDER = "KTO"
    }
}
