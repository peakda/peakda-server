package com.peakda.server.domain.festival.repository

import com.peakda.server.domain.festival.entity.Festival
import org.springframework.data.jpa.repository.JpaRepository

interface FestivalRepository : JpaRepository<Festival, Long> {
    fun findByFstvlNmAndOparAndFstvlStartDate(fstvlNm: String, opar: String, fstvlStartDate: String): Festival?
}
