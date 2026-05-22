package com.peakda.server.domain.spot.repository

import com.peakda.server.domain.spot.entity.Plant
import com.peakda.server.domain.spot.entity.PlantStatus
import org.springframework.data.jpa.repository.JpaRepository

interface PlantRepository : JpaRepository<Plant, Long> {
    fun findAllByStatusOrderBySortOrderAscIdAsc(status: PlantStatus): List<Plant>
    fun findAllByStatusAndNameContainingIgnoreCaseOrderBySortOrderAscIdAsc(
        status: PlantStatus,
        keyword: String,
    ): List<Plant>
    fun existsByName(name: String): Boolean
    fun countBySuggestedByUserIdAndCreatedAtAfter(
        suggestedByUserId: Long,
        createdAtAfter: java.time.Instant,
    ): Long
}
