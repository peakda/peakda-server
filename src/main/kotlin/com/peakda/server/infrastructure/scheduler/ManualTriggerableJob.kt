package com.peakda.server.infrastructure.scheduler

/**
 * 백오피스에서 관리자가 수동으로 실행할 수 있는 잡.
 *
 * 크론 진입점(`run`)은 이 인터페이스에 두지 않는다. 크론은 `enabled` 설정을 존중하지만
 * 수동 실행은 그 설정과 무관하게 동작해야 하므로, 두 진입점을 분리하고 작업 본문만 공유한다.
 */
interface ManualTriggerableJob {
    val jobName: String

    /** 관리자 수동 실행. `enabled` 설정과 무관하게 실행되며 실행 이력을 남긴다. */
    fun runNow()
}
