package com.peakda.server.infrastructure.scheduler

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class ManualJobExecutor {

    @Async
    fun execute(job: ManualTriggerableJob) {
        job.runNow()
    }
}
