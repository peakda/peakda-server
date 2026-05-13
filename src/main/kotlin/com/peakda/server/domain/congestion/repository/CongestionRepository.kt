package com.peakda.server.domain.congestion.repository

import com.peakda.server.domain.congestion.entity.Congestion
import org.springframework.data.jpa.repository.JpaRepository

interface CongestionRepository : JpaRepository<Congestion, Long> {
    fun findByBaseYmdAndTAtsCd(baseYmd: String, tAtsCd: String): Congestion?
}
