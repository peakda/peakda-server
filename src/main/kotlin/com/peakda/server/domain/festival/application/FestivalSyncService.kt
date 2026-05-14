package com.peakda.server.domain.festival.application

import com.peakda.server.domain.festival.repository.FestivalRepository
import com.peakda.server.infrastructure.external.pubdata.festival.response.FestivalItem
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FestivalSyncService(
    private val repository: FestivalRepository,
) {
    @Transactional
    fun upsertPage(items: List<FestivalItem>): Int {
        return items
            .filter { it.fstvlNm.isNotBlank() && it.opar.isNotBlank() && it.fstvlStartDate.isNotBlank() }
            .sumOf { repository.upsert(it.toUpsertCommand()) }
    }
}
