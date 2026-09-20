package com.peakda.server.common.storage

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class ObjectKeyUrlResolverTest {

    private val objectStorage = mock(ObjectStorage::class.java)

    @Test
    fun `공개 base URL 이 설정되면 만료 없는 고정 URL 을 쓴다`() {
        val resolver = resolver(publicBaseUrl = "https://cdn.peakda.com")

        val url = resolver.resolveKey("spot-records/7/2026/09/uuid/main.jpg")

        assertThat(url).isEqualTo("https://cdn.peakda.com/spot-records/7/2026/09/uuid/main.jpg")
        verify(objectStorage, never()).presignedGetUrl("spot-records/7/2026/09/uuid/main.jpg")
    }

    @Test
    fun `같은 key 는 항상 같은 URL 이어야 CDN 캐시가 맞는다`() {
        val resolver = resolver(publicBaseUrl = "https://cdn.peakda.com")

        val first = resolver.resolveKey("profile-images/42/main.jpg")
        val second = resolver.resolveKey("profile-images/42/main.jpg")

        assertThat(first).isEqualTo(second)
    }

    @Test
    fun `공개 base URL 의 끝 슬래시와 key 의 앞 슬래시가 겹치지 않는다`() {
        val resolver = resolver(publicBaseUrl = "https://cdn.peakda.com/")

        val url = resolver.resolveKey("/profile-images/42/main.jpg")

        assertThat(url).isEqualTo("https://cdn.peakda.com/profile-images/42/main.jpg")
    }

    @Test
    fun `공개 base URL 이 없으면 presigned URL 로 폴백한다`() {
        `when`(objectStorage.presignedGetUrl("profile-images/42/main.jpg"))
            .thenReturn("https://s3/profile-images/42/main.jpg?X-Amz-Signature=aaa")
        val resolver = resolver(publicBaseUrl = "")

        val url = resolver.resolveKey("profile-images/42/main.jpg")

        assertThat(url).isEqualTo("https://s3/profile-images/42/main.jpg?X-Amz-Signature=aaa")
    }

    @Test
    fun `외부 URL 은 그대로 노출한다`() {
        val resolver = resolver(publicBaseUrl = "https://cdn.peakda.com")

        val url = resolver.resolve("https://k.kakaocdn.net/profile.jpg")

        assertThat(url).isEqualTo("https://k.kakaocdn.net/profile.jpg")
    }

    @Test
    fun `비어 있는 값은 null 을 돌려준다`() {
        val resolver = resolver(publicBaseUrl = "https://cdn.peakda.com")

        assertThat(resolver.resolve(null)).isNull()
        assertThat(resolver.resolve(" ")).isNull()
    }

    private fun resolver(publicBaseUrl: String) =
        ObjectKeyUrlResolver(
            objectStorage,
            StorageProperties(bucket = "peakda-bucket", publicBaseUrl = publicBaseUrl),
        )
}
