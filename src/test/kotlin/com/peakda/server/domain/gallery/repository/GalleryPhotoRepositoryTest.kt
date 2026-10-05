package com.peakda.server.domain.gallery.repository

import com.peakda.server.common.test.IntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

@Transactional
class GalleryPhotoRepositoryTest : IntegrationTestSupport() {

    @Autowired
    lateinit var repository: GalleryPhotoRepository

    @Test
    fun `제목이나 검색 키워드가 정규식에 맞는 사진만 읽는다`() {
        upsert("g1", title = "마곡사", keyword = "마곡사, 단풍, 가을")
        upsert("g2", title = "진해 벚꽃", keyword = null)
        upsert("g3", title = "해운대 야경", keyword = "해운대, 야경")

        val photos = repository.findByTextMatching("단풍|벚꽃")

        assertThat(photos.map { it.tourApiContentId }).containsExactlyInAnyOrder("g1", "g2")
    }

    private fun upsert(contentId: String, title: String?, keyword: String?) {
        repository.upsert(
            GalleryPhotoUpsertCommand(
                tourApiContentId = contentId,
                contentTypeCode = null,
                title = title,
                webImageUrl = null,
                externalCreatedAt = null,
                externalModifiedAt = null,
                photographyMonth = null,
                photographyLocation = null,
                photographer = null,
                searchKeyword = keyword,
            ),
        )
    }
}
