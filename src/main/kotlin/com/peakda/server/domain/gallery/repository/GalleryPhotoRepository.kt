package com.peakda.server.domain.gallery.repository

import com.peakda.server.domain.gallery.entity.GalleryPhoto
import org.springframework.data.jpa.repository.JpaRepository

interface GalleryPhotoRepository : JpaRepository<GalleryPhoto, Long> {
    fun findByTourApiContentId(tourApiContentId: String): GalleryPhoto?
}
