package com.peakda.server.domain.festival.entity

/**
 * 축제 종류. [TREE]는 운영 기간으로 단계를 나누는 트리 명소로, 관리자가 직접 등록한다.
 *
 * 축제 상세·주변 축제·sitemap 공개 조회는 두 종류를 모두 노출하고 응답의 `kind`로 구분한다.
 * 탐색 화면의 꽃 축제 목록, 꽃 태깅·개화 추정, 방문 추천 이유(축제 진행 중)는 [FESTIVAL]만 대상으로 한다.
 */
enum class FestivalKind {
    FESTIVAL,
    TREE,
}
