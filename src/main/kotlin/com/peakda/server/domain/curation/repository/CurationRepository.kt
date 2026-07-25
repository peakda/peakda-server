package com.peakda.server.domain.curation.repository

import com.peakda.server.domain.curation.entity.Curation
import com.peakda.server.domain.curation.entity.CurationStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface CurationRepository : JpaRepository<Curation, Long> {

    /** 발행된 큐레이션을 최신 주차순으로 (탐색 카드·목록). */
    fun findByStatusOrderByWeekStartDateDesc(status: CurationStatus, pageable: Pageable): Page<Curation>

    fun findByIdAndStatus(id: Long, status: CurationStatus): Curation?

    fun findByWeekStartDate(weekStartDate: LocalDate): Curation?
}
