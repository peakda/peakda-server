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
        return items
            .filter { it.galContentId.isNotBlank() }
            .sumOf { repository.upsert(it.toUpsertCommand()) }
    }
}
