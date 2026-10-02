package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.gallery.repository.GalleryPhotoRepository
import com.peakda.server.domain.seasonal.entity.BloomCategory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 관광사진 갤러리에서 꽃 사진을 읽어 [GalleryFlowerIndex] 를 만든다.
 *
 * 갤러리 사진은 "그 장소에서 그 꽃을 실제로 찍었다"는 기록이라, 장소 유형만 보는 분류 신호보다 강한 근거다.
 */
@Service
class GalleryFlowerEvidenceService(
    private val galleryPhotoRepository: GalleryPhotoRepository,
) {
    @Transactional(readOnly = true)
    fun loadIndex(): GalleryFlowerIndex {
        val photos = galleryPhotoRepository.findByTextMatching(FLOWER_PATTERN)
            .mapNotNull { photo ->
                GalleryFlowerPhoto.of(
                    contentId = photo.tourApiContentId,
                    title = photo.title,
                    keywords = photo.searchKeyword,
                    location = photo.photographyLocation,
                )
            }
        return GalleryFlowerIndex(photos)
    }

    companion object {
        /** DB 에서 후보를 좁히는 꽃 이름 정규식. 제외어 판정은 [GalleryFlowerPhoto.of] 가 한다. */
        private val FLOWER_PATTERN = BloomCategory.entries.flatMap { it.keywordHints }.distinct().joinToString("|")
    }
}
