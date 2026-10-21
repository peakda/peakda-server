package com.peakda.server.domain.launchalert.entity

enum class LaunchAlertDispatchStatus {
    /** 스케줄러 잡이 묶음 단위로 보내는 중 */
    DISPATCHING,

    /** 남은 신청을 끝까지 한 번씩 시도했다. 실패한 신청은 남아 있을 수 있다 */
    COMPLETED,

    /** 연속 실패로 중단했다. 남은 신청은 그대로다 */
    FAILED,

    /** 관리자가 멈췄다. 남은 신청은 그대로다 */
    CANCELED,
}
