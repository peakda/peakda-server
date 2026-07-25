package com.peakda.server.domain.festival.application

/** SCR-027 상태 뱃지. 시작 전=UPCOMING(D-N), 진행 중=ONGOING, 종료 임박=ENDING_SOON(종료 D-N), 종료=ENDED. */
enum class FestivalPhase {
    UPCOMING,
    ONGOING,
    ENDING_SOON,
    ENDED,
}
