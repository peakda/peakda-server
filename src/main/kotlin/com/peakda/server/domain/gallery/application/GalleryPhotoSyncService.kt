package com.peakda.server.domain.gallery.application

import com.peakda.server.domain.gallery.repository.GalleryPhotoRepository
import com.peakda.server.infrastructure.external.kto.photo.response.GalleryListItem
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GalleryPhotoSyncService(
    private val repository: GalleryPhotoRepository,
) {
    @Transactional
    fun upsertPage(items: List<GalleryListItem>): Int {
        var saved = 0
        for (item in items) {
            if (item.galContentId.isBlank()) continue
            val existing = repository.findByTourApiContentId(item.galContentId)
            if (existing == null) repository.save(item.toGalleryPhoto()) else existing.applyUpdate(item)
            saved++
        }
        return saved
    }
}
