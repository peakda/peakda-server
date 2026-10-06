package com.peakda.server.domain.attraction.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class AttractionTest {

    @Test
    fun `썸네일을 아직 확인하지 않았으면 썸네일을 쓴다`() {
        assertThat(attraction().cardImageUrl()).isEqualTo(THUMBNAIL)
    }

    @Test
    fun `썸네일이 열린다고 확인됐으면 썸네일을 쓴다`() {
        val attraction = attraction(checkedUrl = THUMBNAIL, missing = false)

        assertThat(attraction.cardImageUrl()).isEqualTo(THUMBNAIL)
    }

    @Test
    fun `썸네일 파일이 없다고 확인됐으면 원본을 쓴다`() {
        val attraction = attraction(checkedUrl = THUMBNAIL, missing = true)

        assertThat(attraction.cardImageUrl()).isEqualTo(PRIMARY)
    }

    @Test
    fun `없다고 확인한 뒤 썸네일 URL 이 바뀌면 새 썸네일을 쓴다`() {
        val attraction = attraction(thumbnail = "http://tong.visitkorea.or.kr/cms/resource/30/new_image3_1.jpg", checkedUrl = THUMBNAIL, missing = true)

        assertThat(attraction.cardImageUrl()).isEqualTo("http://tong.visitkorea.or.kr/cms/resource/30/new_image3_1.jpg")
    }

    @Test
    fun `썸네일이 없으면 원본을 쓴다`() {
        assertThat(attraction(thumbnail = null).cardImageUrl()).isEqualTo(PRIMARY)
    }

    @Test
    fun `썸네일이 깨졌고 원본도 없으면 null 이다`() {
        val attraction = attraction(primary = null, checkedUrl = THUMBNAIL, missing = true)

        assertThat(attraction.cardImageUrl()).isNull()
    }

    private fun attraction(
        thumbnail: String? = THUMBNAIL,
        primary: String? = PRIMARY,
        checkedUrl: String? = null,
        missing: Boolean = false,
    ) = Attraction(
        tourApiContentId = "2614830",
        title = "신선대와 억새평전",
        primaryImageUrl = primary,
        thumbnailImageUrl = thumbnail,
        thumbnailCheckedUrl = checkedUrl,
        thumbnailCheckedAt = checkedUrl?.let { Instant.parse("2026-10-06T00:00:00Z") },
        thumbnailMissing = missing,
    )

    companion object {
        private const val THUMBNAIL = "http://tong.visitkorea.or.kr/cms/resource/30/2614830_image3_1.bmp"
        private const val PRIMARY = "http://tong.visitkorea.or.kr/cms/resource/30/2614830_image2_1.bmp"
    }
}
