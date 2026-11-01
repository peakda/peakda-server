package com.peakda.server.domain.festival.entity

/**
 * 축제 종류. [TREE]는 운영 기간으로 단계를 나누는 트리 명소로, 관리자가 직접 등록한다.
 *
 * 트리 명소는 앱 화면이 정해지기 전이라 공개 조회(탐색·축제 상세·주변 축제·sitemap)와
 * 꽃 태깅·개화 추정은 [FESTIVAL]만 대상으로 한다.
 */
enum class FestivalKind {
    FESTIVAL,
    TREE,
}
