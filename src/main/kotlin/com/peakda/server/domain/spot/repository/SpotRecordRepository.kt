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
    fun findBySpotIdInAndStatus(spotIds: Collection<Long>, status: SpotRecordStatus): List<SpotRecord>
    fun countBySpotIdAndStatus(spotId: Long, status: SpotRecordStatus): Long
    fun findBySpotIdAndStatusOrderByCreatedAtDesc(
        spotId: Long,
        status: SpotRecordStatus,
        pageable: Pageable,
    ): Page<SpotRecord>
    fun findByUserIdAndStatusOrderByCreatedAtDesc(
        userId: Long,
        status: SpotRecordStatus,
        pageable: Pageable,
    ): Page<SpotRecord>

    /** 전체 피드(SCR-023 "전체" 탭) — 게시된 모든 기록. */
    fun findByStatus(status: SpotRecordStatus, pageable: Pageable): Page<SpotRecord>

    /** 전체 피드 — 차단한 작성자를 제외한 게시된 기록. */
    fun findByStatusAndUserIdNotIn(status: SpotRecordStatus, excludedUserIds: Collection<Long>, pageable: Pageable): Page<SpotRecord>

    /** 팔로잉 피드 — 지정한 작성자(주로 팔로잉 대상) 목록의 게시된 기록. */
    fun findByUserIdInAndStatus(userIds: Collection<Long>, status: SpotRecordStatus, pageable: Pageable): Page<SpotRecord>

    /** 관심 식물 피드 — 미리 추려둔 기록 id 집합 중 게시된 것. */
    fun findByIdInAndStatus(ids: Collection<Long>, status: SpotRecordStatus, pageable: Pageable): Page<SpotRecord>
}
