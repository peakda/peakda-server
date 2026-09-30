package com.peakda.server.domain.congestion.repository

import com.peakda.server.domain.congestion.entity.CongestionAttractionLink
import com.peakda.server.domain.congestion.entity.CongestionLinkStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface CongestionAttractionLinkRepository : JpaRepository<CongestionAttractionLink, Long> {
    fun findByAreaCodeAndSigunguCode(areaCode: String, sigunguCode: String): List<CongestionAttractionLink>

    fun findByAttractionIdAndStatusOrderByIdAsc(
        attractionId: Long,
        status: CongestionLinkStatus,
    ): List<CongestionAttractionLink>

    fun findByStatusOrderByIdAsc(status: CongestionLinkStatus, pageable: Pageable): Page<CongestionAttractionLink>

    fun countByStatus(status: CongestionLinkStatus): Long
}
