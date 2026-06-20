package com.peakda.server.domain.spot.repository

import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface SpotRecordRepository : JpaRepository<SpotRecord, Long> {
    fun findByUserId(userId: Long): List<SpotRecord>
    fun findByUserIdAndStatus(userId: Long, status: SpotRecordStatus): SpotRecord?
    fun findBySpotId(spotId: Long, pageable: Pageable): Page<SpotRecord>
    fun findByUserIdAndStatusOrderByCreatedAtDesc(
        userId: Long,
        status: SpotRecordStatus,
        pageable: Pageable,
    ): Page<SpotRecord>
}
