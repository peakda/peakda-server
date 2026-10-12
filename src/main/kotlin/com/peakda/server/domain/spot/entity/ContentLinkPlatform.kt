package com.peakda.server.domain.spot.entity

/**
 * 기록에 붙인 관련 콘텐츠 링크의 플랫폼. 클라이언트가 고르지 않고 서버가 URL hostname 으로 분류한다.
 * 목록에 없는 도메인은 [UNSUPPORTED] 로 저장해 URL 만 노출한다.
 */
enum class ContentLinkPlatform {
    INSTAGRAM,
    NAVER_BLOG,
    YOUTUBE,
    UNSUPPORTED,
}
