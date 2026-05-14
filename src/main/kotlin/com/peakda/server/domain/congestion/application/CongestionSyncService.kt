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
        return items
            .filter { it.baseYmd.isNotBlank() && it.tAtsCd.isNotBlank() }
            .sumOf { repository.upsert(it.toUpsertCommand()) }
    }
}
