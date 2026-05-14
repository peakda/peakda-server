package com.peakda.server.domain.congestion.application

import com.peakda.server.domain.congestion.repository.CongestionRepository
import com.peakda.server.infrastructure.external.kto.tatscnctr.response.CnctrRateItem
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CongestionSyncService(
    private val repository: CongestionRepository,
) {
    @Transactional
    fun upsertPage(items: List<CnctrRateItem>): Int {
        var saved = 0
        for (item in items) {
            if (item.baseYmd.isBlank() || item.tAtsCd.isBlank()) continue
            val existing = repository.findByBaseDateAndTouristAttractionCode(item.baseYmd, item.tAtsCd)
            if (existing == null) repository.save(item.toCongestion()) else existing.applyUpdate(item)
            saved++
        }
        return saved
    }
}
