package com.peakda.server.domain.user.application

import com.peakda.server.common.storage.ObjectKeyUrlResolver
import com.peakda.server.common.storage.ObjectStorage
import com.peakda.server.common.storage.StorageProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class ProfileImageUrlResolverTest {

    private val resolver = ProfileImageUrlResolver(
        ObjectKeyUrlResolver(
            mock(ObjectStorage::class.java),
            StorageProperties(bucket = "peakda-bucket", publicBaseUrl = CDN),
        ),
    )

    @Test
    fun `아바타 자리에는 128px 썸네일을 내려준다`() {
        assertThat(resolver.thumbnailUrl("profile-images/42/main.jpg"))
            .isEqualTo("$CDN/profile-images/42/thumbnail.jpg")
    }

    @Test
    fun `프로필 상세는 512px 원본과 사이즈별 URL 을 함께 내려준다`() {
        assertThat(resolver.mainUrl("profile-images/42/main.jpg"))
            .isEqualTo("$CDN/profile-images/42/main.jpg")
        assertThat(resolver.variantUrls("profile-images/42/main.jpg")).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "thumbnail" to "$CDN/profile-images/42/thumbnail.jpg",
                "main" to "$CDN/profile-images/42/main.jpg",
            ),
        )
    }

    @Test
    fun `가입 중 임시 업로드도 같은 prefix 규칙을 따른다`() {
        assertThat(resolver.thumbnailUrl("temp/signup/7/main.jpg"))
            .isEqualTo("$CDN/temp/signup/7/thumbnail.jpg")
    }

    @Test
    fun `외부 OAuth 이미지는 고를 사이즈가 없어 그대로 내려준다`() {
        val external = "https://k.kakaocdn.net/profile.jpg"

        assertThat(resolver.thumbnailUrl(external)).isEqualTo(external)
        assertThat(resolver.mainUrl(external)).isEqualTo(external)
        assertThat(resolver.variantUrls(external)).isEmpty()
    }

    @Test
    fun `프로필 이미지가 없으면 null 과 빈 맵을 돌려준다`() {
        assertThat(resolver.thumbnailUrl(null)).isNull()
        assertThat(resolver.mainUrl(null)).isNull()
        assertThat(resolver.variantUrls(null)).isEmpty()
    }

    companion object {
        private const val CDN = "https://cdn.peakda.com"
    }
}
