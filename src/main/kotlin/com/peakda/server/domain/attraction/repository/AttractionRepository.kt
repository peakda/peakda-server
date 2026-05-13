package com.peakda.server.domain.attraction.repository

import com.peakda.server.domain.attraction.entity.Attraction
import org.springframework.data.jpa.repository.JpaRepository

interface AttractionRepository : JpaRepository<Attraction, Long> {
    fun findByContentId(contentId: String): Attraction?
}
