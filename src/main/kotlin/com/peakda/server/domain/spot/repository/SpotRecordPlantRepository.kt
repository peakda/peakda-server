package com.peakda.server.domain.spot.repository

import com.peakda.server.domain.spot.entity.SpotRecordPlant
import com.peakda.server.domain.spot.entity.SpotRecordPlantId
import org.springframework.data.jpa.repository.JpaRepository

interface SpotRecordPlantRepository : JpaRepository<SpotRecordPlant, SpotRecordPlantId> {
    fun findByIdSpotRecordId(spotRecordId: Long): List<SpotRecordPlant>
    fun findByIdSpotRecordIdIn(spotRecordIds: Collection<Long>): List<SpotRecordPlant>
    fun deleteByIdSpotRecordId(spotRecordId: Long)

    /** 관심 식물 피드(결정 B) — 카테고리에서 확장한 Plant id 묶음이 태깅된 기록 id 들을 역으로 찾는다. */
    fun findByIdPlantIdIn(plantIds: Collection<Long>): List<SpotRecordPlant>
}
