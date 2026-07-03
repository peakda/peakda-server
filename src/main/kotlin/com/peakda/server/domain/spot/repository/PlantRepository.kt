package com.peakda.server.domain.spot.repository

import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.spot.entity.Plant
import com.peakda.server.domain.spot.entity.PlantStatus
import org.springframework.data.jpa.repository.JpaRepository

interface PlantRepository : JpaRepository<Plant, Long> {
    fun findAllByStatusOrderBySortOrderAscIdAsc(status: PlantStatus): List<Plant>
    fun findAllByStatusAndNameContainingIgnoreCaseOrderBySortOrderAscIdAsc(
        status: PlantStatus,
        keyword: String,
    ): List<Plant>
    fun existsByNameIgnoreCase(name: String): Boolean

    /** 관심 꽃(BloomCategory) 브릿지 — 피드 "관심 식물" 필터(결정 B)에서 카테고리 → 세부 Plant 로 확장할 때 쓴다. */
    fun findByBloomCategoryIn(categories: Collection<BloomCategory>): List<Plant>
}
