package com.peakda.server.domain.attraction.repository

import com.peakda.server.domain.attraction.entity.Attraction
import org.springframework.data.jpa.repository.JpaRepository

interface AttractionRepository : JpaRepository<Attraction, Long> {
    fun findByTourApiContentId(tourApiContentId: String): Attraction?
}
