package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.seasonal.repository.BloomObservationRepository
import com.peakda.server.infrastructure.external.kma.flower.response.FlowerDetail
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BloomObservationSyncService(
    private val repository: BloomObservationRepository,
) {
    @Transactional
    fun upsert(detail: FlowerDetail): Int {
        return detail.toUpsertCommand()?.let(repository::upsert) ?: 0
    }
}
