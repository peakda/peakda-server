package com.peakda.server.domain.festival.entity

/** 축제 행의 출처. 공공데이터 동기화는 [MANUAL] 행을 덮어쓰지 않는다. */
enum class FestivalSource {
    PUBLIC_DATA,
    MANUAL,
}
