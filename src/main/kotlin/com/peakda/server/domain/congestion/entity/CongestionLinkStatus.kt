package com.peakda.server.domain.congestion.entity

/** 집중률 관광지명 ↔ 명소 연결 상태. 화면에는 [CONFIRMED] 만 노출한다. */
enum class CongestionLinkStatus {
    /** 확정 — 자동 매칭(유일 정확 일치) 또는 관리자 확정. 매칭 잡이 다시 건드리지 않는다. */
    CONFIRMED,

    /** 검토 대기 — 후보가 여럿이거나 이름이 포함 관계로만 맞는 경우. 매칭 잡이 매번 다시 평가한다. */
    PENDING_REVIEW,

    /** 관리자 거절 — 매칭 잡이 다시 건드리지 않는다. */
    REJECTED,

    /** 후보 없음 — 새 명소가 적재되면 다시 평가한다. */
    UNMATCHED,
    ;

    /** 매칭 잡이 덮어써도 되는 상태인지. 확정·거절은 사람이 내린 결정이거나 이미 신뢰하는 결과다. */
    val reevaluable: Boolean
        get() = this == PENDING_REVIEW || this == UNMATCHED
}
