package com.peakda.server.domain.spot.repository

import com.peakda.server.domain.spot.entity.SpotRecordPhoto
import org.springframework.data.jpa.repository.JpaRepository

interface SpotRecordPhotoRepository : JpaRepository<SpotRecordPhoto, Long> {
    fun findBySpotRecordIdOrderBySortOrderAsc(spotRecordId: Long): List<SpotRecordPhoto>
    fun findBySpotRecordIdIn(spotRecordIds: Collection<Long>): List<SpotRecordPhoto>
    fun deleteBySpotRecordId(spotRecordId: Long)
}
