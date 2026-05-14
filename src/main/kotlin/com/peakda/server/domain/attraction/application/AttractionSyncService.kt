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
        return items
            .filter { it.contentid.isNotBlank() }
            .sumOf { repository.upsert(it.toUpsertCommand()) }
    }
}
