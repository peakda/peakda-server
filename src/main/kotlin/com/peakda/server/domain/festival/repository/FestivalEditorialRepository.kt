package com.peakda.server.domain.festival.repository

import com.peakda.server.domain.festival.entity.FestivalEditorial
import com.peakda.server.domain.festival.entity.FestivalEditorialStatus
import org.springframework.data.jpa.repository.JpaRepository

interface FestivalEditorialRepository : JpaRepository<FestivalEditorial, Long> {
    fun findByFestivalId(festivalId: Long): FestivalEditorial?

    fun findByFestivalIdAndStatus(
        festivalId: Long,
        status: FestivalEditorialStatus,
    ): FestivalEditorial?
}
