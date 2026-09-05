package com.peakda.server.infrastructure.external.kto.tatscnctr

import com.fasterxml.jackson.databind.ObjectMapper
import com.peakda.server.infrastructure.external.datagokr.DataGoKrBody
import com.peakda.server.infrastructure.external.datagokr.DataGoKrErrorDecoder
import com.peakda.server.infrastructure.external.common.ExternalApiResilienceExecutor
import com.peakda.server.infrastructure.external.datagokr.getDataGoKrBody
import com.peakda.server.infrastructure.external.kto.tatscnctr.response.CnctrRateItem
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class TatsCnctrClient(
    @param:Qualifier("tatsCnctrRestClient") private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
    private val errorDecoder: DataGoKrErrorDecoder,
    private val resilience: ExternalApiResilienceExecutor,
) {
    /**
     * 관광지 집중률 목록 조회. areaCd·signguCd 가 필수 파라미터다.
     *
     * 오퍼레이션명은 `tatsCnctrRatedList` 다. 활용매뉴얼 v4.0 의 Call Back URL 항목에는
     * `tatsCnctrRateList` 로 적혀 있지만 게이트웨이는 404 `API not found` 를 돌려준다.
     * 같은 문서의 요청 예제에 있는 `tatsCnctrRatedList` 만 유효하다.
     */
    fun tatsCnctrRatedList(params: Map<String, Any?>): DataGoKrBody<CnctrRateItem> {
        return resilience.execute(PROVIDER) {
            restClient.getDataGoKrBody<CnctrRateItem>(objectMapper, errorDecoder, "/tatsCnctrRatedList", params)
        }
    }

    companion object {
        private const val PROVIDER = "KTO"
    }
}
