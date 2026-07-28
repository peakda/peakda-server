package com.peakda.server.domain.spot.repository

import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

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

    /**
     * 여러 스팟의 게시 기록 수를 한 번에 집계한다. 찜 목록 카드의 "방문 기록 N" 표시용.
     */
    @Query(
        """
            SELECT r.spotId AS spotId, COUNT(r) AS recordCount
            FROM SpotRecord r
            WHERE r.spotId IN :spotIds
              AND r.status = :status
            GROUP BY r.spotId
        """,
    )
    fun countBySpotIdInAndStatus(
        @Param("spotIds") spotIds: Collection<Long>,
        @Param("status") status: SpotRecordStatus,
    ): List<SpotRecordCount>
}

/** [SpotRecordRepository.countBySpotIdInAndStatus] 프로젝션. */
interface SpotRecordCount {
    val spotId: Long
    val recordCount: Long
}
