package com.peakda.server.domain.spot.application

import com.peakda.server.common.storage.ObjectKeyUrlResolver
import com.peakda.server.common.storage.ObjectStorage
import com.peakda.server.common.storage.StorageProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class SpotRecordPhotoUrlResolverTest {

    private val resolver = SpotRecordPhotoUrlResolver(
        ObjectKeyUrlResolver(
            mock(ObjectStorage::class.java),
            StorageProperties(bucket = "peakda-bucket", publicBaseUrl = CDN),
        ),
    )

    @Test
    fun `variant 를 모두 보유한 사진은 각 사이즈의 실제 key 를 돌려준다`() {
        val urls = resolver.variantUrls(MAIN_KEY, "thumbnail,medium,main")

        assertThat(urls).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "thumbnail" to "$CDN/$PREFIX/thumbnail.jpg",
                "medium" to "$CDN/$PREFIX/medium.jpg",
                "main" to "$CDN/$PREFIX/main.jpg",
            ),
        )
    }

    @Test
    fun `백필 전 사진은 없는 medium 대신 원본 URL 을 돌려준다`() {
        val urls = resolver.variantUrls(MAIN_KEY, null)

        assertThat(urls["medium"]).isEqualTo("$CDN/$PREFIX/main.jpg")
        assertThat(urls["thumbnail"]).isEqualTo("$CDN/$PREFIX/thumbnail.jpg")
        assertThat(urls["main"]).isEqualTo("$CDN/$PREFIX/main.jpg")
    }

    @Test
    fun `카드 썸네일은 thumbnail variant 를 가리킨다`() {
        assertThat(resolver.thumbnailUrl(MAIN_KEY)).isEqualTo("$CDN/$PREFIX/thumbnail.jpg")
    }

    @Test
    fun `원본 URL 은 저장된 key 그대로를 가리킨다`() {
        assertThat(resolver.mainUrl(MAIN_KEY)).isEqualTo("$CDN/$PREFIX/main.jpg")
    }

    companion object {
        private const val CDN = "https://cdn.peakda.com"
        private const val PREFIX = "spot-records/42/2026/09/9b1deb4d"
        private const val MAIN_KEY = "$PREFIX/main.jpg"
    }
}
