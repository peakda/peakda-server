package com.peakda.server.domain.visitor.repository

import com.peakda.server.domain.visitor.entity.RegionVisitor
import org.springframework.data.jpa.repository.JpaRepository

interface RegionVisitorRepository : JpaRepository<RegionVisitor, Long> {
    fun findByBaseYmdAndAreaCdAndTouDivCd(baseYmd: String, areaCd: String, touDivCd: String): RegionVisitor?
}
