package com.peakda.server.domain.congestion.application

import com.peakda.server.domain.congestion.repository.CongestionRepository
import com.peakda.server.infrastructure.external.kto.tatscnctr.response.CnctrRateItem
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CongestionSyncService(
    private val repository: CongestionRepository,
) {
    /** 자연키 (기준일자, 지역, 시군구, 관광지명) 가 온전한 항목만 적재한다. */
    @Transactional
    fun upsertPage(items: List<CnctrRateItem>): Int {
        return items
            .filter {
                it.baseYmd.isNotBlank() &&
                    it.areaCd.isNotBlank() &&
                    it.signguCd.isNotBlank() &&
                    it.tAtsNm.isNotBlank()
            }
            .sumOf { repository.upsert(it.toUpsertCommand()) }
    }
}
