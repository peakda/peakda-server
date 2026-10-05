package com.peakda.server.domain.festival.repository

import com.peakda.server.domain.festival.entity.FestivalEditorial
import com.peakda.server.domain.festival.entity.FestivalEditorialStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface FestivalEditorialRepository : JpaRepository<FestivalEditorial, Long> {
    fun findByFestivalId(festivalId: Long): FestivalEditorial?

    fun findByFestivalIdIn(festivalIds: Collection<Long>): List<FestivalEditorial>

    fun findByFestivalIdInAndStatus(
        festivalIds: Collection<Long>,
        status: FestivalEditorialStatus,
    ): List<FestivalEditorial>

    /** [status] 에디토리얼의 축제 id·수정 시각 (sitemap). */
    fun findByStatus(status: FestivalEditorialStatus): List<FestivalEditorialLastModifiedAt>

    fun findByFestivalIdAndStatus(
        festivalId: Long,
        status: FestivalEditorialStatus,
    ): FestivalEditorial?
}

/** [FestivalEditorialRepository.findByStatus] 프로젝션. */
interface FestivalEditorialLastModifiedAt {
    val festivalId: Long
    val updatedAt: Instant
}
