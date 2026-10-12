package com.peakda.server.domain.spot.application

import com.peakda.server.domain.spot.entity.ContentLinkPlatform
import com.peakda.server.domain.spot.exception.SpotRecordInvalidContentLinkException
import java.net.URI
import java.net.URISyntaxException

/**
 * 기록 1건에 붙는 관련 콘텐츠 링크(1개) 규칙.
 * 미리보기 메타데이터는 클라이언트가 조회하고, 서버는 URL 과 hostname 기준 플랫폼만 저장한다.
 */
object SpotRecordContentLinkPolicy {
    const val MAX_URL_LENGTH: Int = 2048

    private val ALLOWED_SCHEMES: Set<String> = setOf("http", "https")
    private val LEADING_SCHEME: Regex = Regex("^[A-Za-z][A-Za-z0-9+.-]*://")

    /** hostname 이 도메인과 같거나 그 서브도메인이면 해당 플랫폼으로 분류한다. 위에서부터 먼저 맞는 규칙을 쓴다. */
    private val PLATFORM_DOMAINS: List<Pair<ContentLinkPlatform, List<String>>> = listOf(
        ContentLinkPlatform.INSTAGRAM to listOf("instagram.com"),
        ContentLinkPlatform.NAVER_BLOG to listOf("blog.naver.com"),
        ContentLinkPlatform.YOUTUBE to listOf("youtube.com", "youtu.be"),
    )

    /**
     * 입력값을 저장할 URL 로 정규화한다. 공백만 있으면 링크 없음(null)이다.
     * 스킴 없이 붙여넣은 `instagram.com/p/...` 는 https 로 보정한다.
     * `user@host` 형태는 표시 문자열로 다른 도메인을 흉내 낼 수 있어 거절한다.
     */
    fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.length > MAX_URL_LENGTH) throw SpotRecordInvalidContentLinkException()
        val url = if (LEADING_SCHEME.containsMatchIn(trimmed)) trimmed else "https://$trimmed"

        val uri = try {
            URI(url)
        } catch (e: URISyntaxException) {
            throw SpotRecordInvalidContentLinkException()
        }
        val scheme = uri.scheme?.lowercase()
        val host = uri.host
        if (scheme !in ALLOWED_SCHEMES || uri.rawUserInfo != null || host.isNullOrBlank() || !host.contains('.')) {
            throw SpotRecordInvalidContentLinkException()
        }
        return url
    }

    /** [normalize] 를 통과한 URL 의 플랫폼을 분류한다. host 를 읽을 수 없으면 미지원으로 본다. */
    fun classify(normalizedUrl: String): ContentLinkPlatform {
        val host = runCatching { URI(normalizedUrl).host }.getOrNull()
            ?.lowercase()
            ?.trimEnd('.')
            ?: return ContentLinkPlatform.UNSUPPORTED
        return PLATFORM_DOMAINS
            .firstOrNull { (_, domains) -> domains.any { host == it || host.endsWith(".$it") } }
            ?.first
            ?: ContentLinkPlatform.UNSUPPORTED
    }
}
