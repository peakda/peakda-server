package com.peakda.server.domain.spot.repository

import com.peakda.server.domain.spot.entity.SpotRecordPlant
import com.peakda.server.domain.spot.entity.SpotRecordPlantId
import org.springframework.data.jpa.repository.JpaRepository

interface SpotRecordPlantRepository : JpaRepository<SpotRecordPlant, SpotRecordPlantId> {
    fun findByIdSpotRecordId(spotRecordId: Long): List<SpotRecordPlant>
    fun findByIdSpotRecordIdIn(spotRecordIds: Collection<Long>): List<SpotRecordPlant>
    fun deleteByIdSpotRecordId(spotRecordId: Long)
}
