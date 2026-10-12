package com.peakda.server.domain.spot.application

import com.peakda.server.domain.spot.entity.ContentLinkPlatform
import com.peakda.server.domain.spot.exception.SpotRecordInvalidContentLinkException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class SpotRecordContentLinkPolicyTest {

    @Test
    fun `hostname 과 서브도메인으로 플랫폼을 분류한다`() {
        val cases = mapOf(
            "https://www.instagram.com/p/C5y7abcDEF/" to ContentLinkPlatform.INSTAGRAM,
            "https://instagram.com/louin_travel" to ContentLinkPlatform.INSTAGRAM,
            "https://blog.naver.com/louin/223456789" to ContentLinkPlatform.NAVER_BLOG,
            "https://m.blog.naver.com/louin/223456789" to ContentLinkPlatform.NAVER_BLOG,
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ" to ContentLinkPlatform.YOUTUBE,
            "https://m.youtube.com/shorts/abc" to ContentLinkPlatform.YOUTUBE,
            "https://youtu.be/dQw4w9WgXcQ" to ContentLinkPlatform.YOUTUBE,
            "https://WWW.YOUTUBE.COM/watch?v=x" to ContentLinkPlatform.YOUTUBE,
        )

        cases.forEach { (url, platform) ->
            assertThat(SpotRecordContentLinkPolicy.classify(url)).`as`(url).isEqualTo(platform)
        }
    }

    @Test
    fun `목록에 없거나 도메인을 흉내 낸 주소는 미지원 플랫폼이다`() {
        listOf(
            "https://naver.com/some",
            "https://cafe.naver.com/club/1",
            "https://notinstagram.com/p/1",
            "https://instagram.com.evil.example/p/1",
            "https://tistory.com/entry/1",
        ).forEach { url ->
            assertThat(SpotRecordContentLinkPolicy.classify(url)).`as`(url).isEqualTo(ContentLinkPlatform.UNSUPPORTED)
        }
    }

    @Test
    fun `스킴 없이 붙여넣은 주소는 https 로 보정하고 앞뒤 공백을 지운다`() {
        assertThat(SpotRecordContentLinkPolicy.normalize("  instagram.com/p/C5y7abc  "))
            .isEqualTo("https://instagram.com/p/C5y7abc")
        assertThat(SpotRecordContentLinkPolicy.normalize("http://youtu.be/x")).isEqualTo("http://youtu.be/x")
        assertThat(SpotRecordContentLinkPolicy.normalize("youtube.com/redirect?q=https://a.example"))
            .isEqualTo("https://youtube.com/redirect?q=https://a.example")
    }

    @Test
    fun `길이 제한은 보정 전 입력 기준이다`() {
        val path = "a".repeat(SpotRecordContentLinkPolicy.MAX_URL_LENGTH - "example.com/".length)
        assertThat(SpotRecordContentLinkPolicy.normalize("example.com/$path")).isEqualTo("https://example.com/$path")
    }

    @Test
    fun `공백만 있으면 링크 없음으로 본다`() {
        assertThat(SpotRecordContentLinkPolicy.normalize("")).isNull()
        assertThat(SpotRecordContentLinkPolicy.normalize("   ")).isNull()
    }

    @Test
    fun `http 계열이 아니거나 host 가 없는 주소는 거절한다`() {
        listOf(
            "ftp://example.com/file",
            "javascript:alert(1)",
            "https://",
            "https://localhost/path",
            "https://exa mple.com",
            "not a url",
            "https://instagram.com@evil.example/p/1",
            "https://user:pw@example.com/p/1",
            "https://mailto:a@b.com",
            "https://example.com/" + "a".repeat(SpotRecordContentLinkPolicy.MAX_URL_LENGTH),
        ).forEach { raw ->
            assertThatThrownBy { SpotRecordContentLinkPolicy.normalize(raw) }
                .`as`(raw)
                .isInstanceOf(SpotRecordInvalidContentLinkException::class.java)
        }
    }
}
