package com.peakda.server.domain.attraction.application

import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.infrastructure.external.kto.korservice.response.AreaBasedSyncListItem
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AttractionSyncService(
    private val repository: AttractionRepository,
) {
    @Transactional
    fun upsertPage(items: List<AreaBasedSyncListItem>): Int {
        for (item in items) {
            if (item.contentid.isBlank()) continue
            val existing = repository.findByTourApiContentId(item.contentid)
            if (existing == null) {
                repository.save(item.toAttraction())
            } else {
                existing.applyUpdate(item)
            }
        }
        return items.count { it.contentid.isNotBlank() }
    }
}
