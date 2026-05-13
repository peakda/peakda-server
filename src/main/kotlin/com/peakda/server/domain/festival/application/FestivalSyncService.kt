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
        var saved = 0
        for (item in items) {
            if (item.fstvlNm.isBlank() || item.opar.isBlank() || item.fstvlStartDate.isBlank()) continue
            val existing = repository.findByFstvlNmAndOparAndFstvlStartDate(
                item.fstvlNm,
                item.opar,
                item.fstvlStartDate,
            )
            if (existing == null) {
                repository.save(item.toFestival())
            } else {
                existing.applyUpdate(item)
            }
            saved++
        }
        return saved
    }
}
